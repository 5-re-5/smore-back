package org.oreo.smore.domain.participant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.participant.dto.ParticipantInfo;
import org.oreo.smore.domain.participant.dto.ParticipantStatusResponse;
import org.oreo.smore.domain.participant.dto.RoomInfo;
import org.oreo.smore.domain.participant.exception.ParticipantException;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studytime.StudyTime;
import org.oreo.smore.domain.studytime.StudyTimeRepository;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final StudyRoomRepository studyRoomRepository;
    private final UserRepository userRepository;
    private final StudyTimeRepository studyTimeRepository;

    // 참가자 등록
    @Transactional
    public Participant joinRoom(Long roomId, Long userId) {
        log.info("참가자 등록 시작 - 방ID: {}, 사용자ID: {} ", roomId, userId);

        // 방 존재 여부 확인
        StudyRoom studyRoom = validateStudyRoomExists(roomId);

        // 이미 참가중인지 확인
        Participant existingParticipant = checkExistingParticipant(roomId, userId);
        if (existingParticipant != null) {
            log.info("✅ 기존 참가자 정보 반환 - 방ID: {}, 사용자ID: {}, 참가자ID: {}",
                    roomId, userId, existingParticipant.getParticipantId());
            return existingParticipant;
        }

        // 방 최대 인원 확인
        validateRoomCapacity(studyRoom);

        // 참가자 엔티티 생성
        Participant participant = Participant.builder()
                .roomId(roomId)
                .userId(userId)
                .build();

        Participant savedParticipant = participantRepository.save(participant);

        long currentCount = participantRepository.countActiveParticipantsByRoomId(roomId);
        log.info("✅ 참가자 등록 완료 - 방ID: {}, 사용자ID: {}, 현재 참가자 수: {}/{}",
                roomId, userId, currentCount, studyRoom.getMaxParticipants());

        return savedParticipant;
    }

    // 참가자 퇴장 처리
    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        log.info("참가자 퇴장 시작 - 방ID: {}, 사용자ID: {} ", roomId, userId);

        Participant participant = findActiveParticipant(roomId, userId);
        participant.leave();

        long remainingCount = participantRepository.countActiveParticipantsByRoomId(roomId);
        log.info("✅ 참가자 퇴장 완료 - 방ID: {}, 사용자ID: {}, 남은 참가자 수: {}",
                roomId, userId, remainingCount);
    }

    // 활성화된 참가자 조회
    private Participant findActiveParticipant(Long roomId, Long userId) {
        List<Participant> activeParticipants = participantRepository.findActiveParticipantsByRoomId(roomId);

        return activeParticipants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("활성 참가자를 찾을 수 없음 - 방ID: {}, 사용자ID: {}", roomId, userId);
                    return new ParticipantException.ParticipantNotFoundException(
                            String.format("방 %d에서 사용자 %d를 찾을 수 없습니다", roomId, userId));
                });
    }


    // 방 최대 인원 검증
    private void validateRoomCapacity(StudyRoom studyRoom) {
        long currentCount = participantRepository.countActiveParticipantsByRoomId(studyRoom.getRoomId());

        if (currentCount >= studyRoom.getMaxParticipants()) {
            log.warn("방 정원 초과 - 방ID: {}, 현재: {}, 최대: {}",
                    studyRoom.getRoomId(), currentCount, studyRoom.getMaxParticipants());
            throw new ParticipantException.RoomFullException(
                    String.format("방이 가득함 (%d/%d)", currentCount, studyRoom.getMaxParticipants()));
        }

    }

    private Participant checkExistingParticipant(Long roomId, Long userId) {
        List<Participant> activeParticipants = getActiveParticipants(roomId);

        Participant existing = activeParticipants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            log.info("기존 활성 참가자 발견 - 방ID: {}, 사용자ID: {}, 참가자ID: {}",
                    roomId, userId, existing.getParticipantId());
        } else {
            log.debug("신규 참가자 - 방ID: {}, 사용자ID: {}", roomId, userId);
        }

        return existing;
    }

    // 이미 참가중인지 검증
    private void validateNotAlreadyInRoom(Long roomId, Long userId) {
        if (isUserInRoom(roomId, userId)) {
            log.warn("이미 참가중인 사용자 - 방ID: {}, 사용자ID: {}", roomId, userId);
            throw new ParticipantException.AlreadyJoinedException(
                    String.format("사용자 %d는 이미 방 %d에 참가중입니다", userId, roomId));
        }
    }

    // 사용자가 특정 방에 참가중인지 확인
    public boolean isUserInRoom(Long roomId, Long userId) {
        List<Participant> activeParticipants = getActiveParticipants(roomId);
        boolean isInRoom = activeParticipants.stream()
                .anyMatch(p -> p.getUserId().equals(userId));

        log.debug("사용자 방 참가 여부 - 방ID: {}, 사용자ID: {}, 참가중: {}", roomId, userId, isInRoom);
        return isInRoom;
    
    }

    public List<Participant> getActiveParticipants(Long roomId) {
        log.debug("현재 활성 참가자 조회 - 방ID: {}", roomId);
        List<Participant> activeParticipants = participantRepository.findActiveParticipantsByRoomId(roomId);
        log.debug("활성 참가자 수: {} - 방ID: {}", activeParticipants.size(), roomId);
        return activeParticipants;

    }

    public long getActiveParticipantCount(Long roomId) {
        long count = participantRepository.countActiveParticipantsByRoomId(roomId);
        log.debug("현재 참가자 수 - 방ID: {}, 참가자 수: {}명", roomId, count);
        return count;
    }

    // 스터디룸 존재 여부 검증
    private StudyRoom validateStudyRoomExists(Long roomId) {
        return studyRoomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 방 - 방ID: {}", roomId);
                    return new ParticipantException.StudyRoomNotFoundException(
                            String.format("방 %d를 찾을 수 없습니다", roomId));
                });
    }

    // 참가자 음소거 설정
    @Transactional
    public void muteParticipant(Long roomId, Long userId) {
        log.info("참가자 음소거 설정 - 방ID: {}, 사용자ID: {}", roomId, userId);

        Participant participant = findActiveParticipant(roomId, userId);
        participant.mute();

        log.info("✅ 참가자 음소거 설정 완료 - 방ID: {}, 사용자ID: {}", roomId, userId);
    }

    // 참가자 음소거 해제
    @Transactional
    public void unmuteParticipant(Long roomId, Long userId) {
        log.info("참가자 음소거 해제 - 방ID: {}, 사용자ID: {}", roomId, userId);

        Participant participant = findActiveParticipant(roomId, userId);
        participant.unmute();

        log.info("✅ 참가자 음소거 해제 완료 - 방ID: {}, 사용자ID: {}", roomId, userId);
    }

    // 참가자 강퇴
    @Transactional
    public void banParticipant(Long roomId, Long userId) {
        log.warn("참가자 강퇴 시작 - 방ID: {}, 사용자ID: {}", roomId, userId);

        Participant participant = findActiveParticipant(roomId, userId);
        participant.ban();

        long remainingCount = participantRepository.countActiveParticipantsByRoomId(roomId);
        log.warn("⚠️ 참가자 강퇴 완료 - 방ID: {}, 사용자ID: {}, 남은 참가자 수: {}",
                roomId, userId, remainingCount);
    }

    // 방장 나가면 방 삭제
    @Transactional
    public void deleteAllParticipantsByRoom(Long roomId) {
        log.warn("방 삭제로 인한 참가 이력 삭제 - 방ID: {}", roomId);

        long participantCount = participantRepository.countActiveParticipantsByRoomId(roomId);
        participantRepository.deleteByRoomId(roomId);

        log.warn("⚠️ 참가 이력 삭제 완료 - 방ID: {}, 삭제된 참가자 수: {}", roomId, participantCount);
    }


    // 통합 상태 조회 메서드
    public ParticipantStatusResponse getParticipantStatus(Long roomId) {
        log.info("참가자 상태 조회 시작 - 방ID: {}", roomId);

        // 방 존재 여부 확인
        StudyRoom studyRoom = validateStudyRoomExists(roomId);

        // 활성화된 참가자 목록 조회
        List<Participant> activeParticipants = getActiveParticipants(roomId);

        if (activeParticipants.isEmpty()) {
            log.warn("활성 참가자가 없는 방 - 방ID: {}", roomId);
            return ParticipantStatusResponse.builder()
                    .participants(List.of())
                    .roomInfo(RoomInfo.builder()
                            .isAllMuted(studyRoom.isAllMuted())
                            .totalParticipants(0)
                            .build())
                    .build();
        }

        // 참가자 정보 변환
        List<ParticipantInfo> participantInfos = activeParticipants.stream()
                .map(participant -> convertToParticipantInfo(participant, studyRoom))
                .collect(Collectors.toList());

        // 방 정보 구성
        RoomInfo roomInfo = RoomInfo.builder()
                .isAllMuted(studyRoom.isAllMuted())
                .totalParticipants(activeParticipants.size())
                .build();

        log.info("✅ 참가자 상태 조회 완료 - 방ID: {}, 참가자 수: {}명, 전체음소거: {}",
                roomId, activeParticipants.size(), studyRoom.isAllMuted());

        return ParticipantStatusResponse.builder()
                .participants(participantInfos)
                .roomInfo(roomInfo)
                .build();
    }

    private ParticipantInfo convertToParticipantInfo(Participant participant, StudyRoom studyRoom) {

        // 사용자 정보 조회
        User user = userRepository.findById(participant.getUserId())
                .orElseThrow(() -> {
                    log.error("사용자를 찾을 수 없음 - 사용자ID: {}", participant.getUserId());
                    return new RuntimeException("사용자를 찾을 수 없습니다: " + participant.getUserId());
                });

        // 방장 여부 확인
        boolean isOwner = studyRoom.getUserId().equals(participant.getUserId());

        // 실제 공부 시간 정보 조회
        int todayStudyTime = getTodayStudyTime(participant.getUserId());
        int targetStudyTime = user.getGoalStudyTime();

        return ParticipantInfo.builder()
                .userId(participant.getUserId())
                .nickname(user.getNickname())
                .isOwner(isOwner)
                .audioEnabled(participant.isAudioEnabled())
                .videoEnabled(participant.isVideoEnabled())
                .todayStudyTime(todayStudyTime)
                .targetStudyTime(targetStudyTime)
                .build();
    }

    // 오늘 공부 시간 조회 로직
    private int getTodayStudyTime(Long userId) {
        try {
            // 오늘 공부 시간 계산
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);

            List<StudyTime> allRecords = studyTimeRepository.findAllByUserIdAndCreatedAtBetween(
                    userId,
                    startOfDay.minusDays(1),  // 하루 전까지도 시작할 수 있으므로
                    endOfDay.plusDays(1)      // 자정 넘어가는 케이스도 포함
            );

            int todayStudyMinutes = 0;

            for (StudyTime record : allRecords) {
                LocalDateTime start = record.getCreatedAt();
                LocalDateTime end = record.getDeletedAt();

                if (end == null) {
                    // 아직 진행 중인 공부 세션은 현재 시간까지로 계산
                    end = LocalDateTime.now();
                }

                // 오늘 날짜 범위와 겹치는 구간만 계산
                LocalDateTime effectiveStart = start.isBefore(startOfDay) ? startOfDay : start;
                LocalDateTime effectiveEnd = end.isAfter(endOfDay) ? endOfDay : end;

                if (!effectiveStart.isAfter(effectiveEnd)) {
                    todayStudyMinutes += Duration.between(effectiveStart, effectiveEnd).toMinutes();
                }
            }

            log.debug("오늘 공부시간 조회 완료 - 사용자ID: {}, 시간: {}분", userId, todayStudyMinutes);
            return todayStudyMinutes;

        } catch (Exception e) {
            log.error("오늘 공부시간 조회 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage());
            return 0; // 오류 시 0분 반환
        }
    }
}
