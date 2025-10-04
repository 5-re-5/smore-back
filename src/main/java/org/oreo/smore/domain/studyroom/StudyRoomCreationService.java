package org.oreo.smore.domain.studyroom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.chat.ChatRoomService;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomRequest;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomResponse;
import org.oreo.smore.domain.studyroom.exception.StudyRoomCreationException;
import org.oreo.smore.domain.studyroom.exception.StudyRoomValidationException;
import org.oreo.smore.global.common.CloudStorageManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyRoomCreationService {

    private final StudyRoomRepository studyRoomRepository;
    private final ChatRoomService chatRoomService;
    private final CloudStorageManager cloudStorageManager;

    @Transactional
    public CreateStudyRoomResponse createStudyRoom(Long userId, CreateStudyRoomRequest request) {
        log.info("스터디룸 생성 요청 시작 - 사용자ID: {}, 제목: [{}], 카테고리: [{}]",
                userId, request.getTitle(), request.getCategory());

        logRequestDetails(request);

        try {
            // 요청 검증
            validateCreateStudyRoomRequest(request);
            log.debug("스터디룸 생성 요청 검증 완료");

            // 초대 해시코드 생성
            String inviteHashCode = generateInviteHashCode();
            log.debug("===초대 해시코드 생성 완료: [{}]===", inviteHashCode);

            // 스터디룸 엔티티 생성
            StudyRoom studyRoom = createStudyRoomEntity(userId, request, inviteHashCode);
            log.debug("===스터디룸 엔티티 생성 완료 - 비밀번호: {}, 타이머: {}, 최대인원: {}명===",
                    request.hasPassword() ? "설정됨" : "없음",
                    request.hasTimerSettings() ? "설정됨" : "없음",
                    studyRoom.getMaxParticipants());

            // 스터디룸 기본 이미지 생성
            studyRoom.setThumbnailUrl("https://oreost.blob.core.windows.net/oreoct/default/room.webp?t=202508130152");

            // DB 저장
            StudyRoom savedStudyRoom = studyRoomRepository.save(studyRoom);
            log.info("===스터디룸 DB 저장 완료 - 방ID: {}===", savedStudyRoom.getRoomId());

            // 이미지 업로드
            if (request.getRoomImage() != null && !request.getRoomImage().isEmpty()) {
                cloudStorageManager.deleteRoomImage(savedStudyRoom.getRoomId());
                try {
                    String uploadedUrl = cloudStorageManager.uploadRoomImage(request.getRoomImage(), savedStudyRoom.getRoomId());
                    savedStudyRoom.setThumbnailUrl(uploadedUrl);
                    savedStudyRoom = studyRoomRepository.save(studyRoom);
                } catch (Exception e) {
                    throw new RuntimeException("프로필 이미지 업로드 실패", e);
                }
            }
            // chatRoom 자동 생성 (StudyRoom 저장 후)
            try {
                chatRoomService.createChatRoom(savedStudyRoom);
                log.info("===✅ 채팅방 자동 생성 완료 - 방ID: {}===", savedStudyRoom.getRoomId());
            } catch (Exception chatException) {
                log.error("❌ 채팅방 생성 실패 (무시됨) - 방ID: {}, 오류: {}",
                        savedStudyRoom.getRoomId(), chatException.getMessage());
            }


            // 응답 생성
            CreateStudyRoomResponse response = CreateStudyRoomResponse.from(savedStudyRoom);

            logCreationStatistics(savedStudyRoom);

            log.info("===✅ 스터디룸 생성 완료 - 방ID: {}, 제목: [{}], 초대코드: [{}]===",
                    savedStudyRoom.getRoomId(), savedStudyRoom.getTitle(), savedStudyRoom.getInviteHashCode());

            return response;

        } catch (StudyRoomValidationException e) {
            log.warn("❌ 스터디룸 생성 요청 검증 실패 - 사용자ID: {}, 오류: {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ 스터디룸 생성 중 예상치 못한 오류 발생 - 사용자ID: {}, 제목: [{}], 오류: {}",
                    userId, request.getTitle(), e.getMessage(), e);
            throw new StudyRoomCreationException("스터디룸 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new StudyRoomValidationException("스터디룸 제목은 필수입니다.");
        }
        if (title.length() > 100) {
            throw new StudyRoomValidationException("스터디룸 제목은 100자 이하여야 합니다.");
        }
        log.debug("제목 검증 통과: [{}]", title);
    }

    // 스터디룸 생성 요청 검증
    private void validateCreateStudyRoomRequest(CreateStudyRoomRequest request) {
        log.debug("스터디룸 생성 요청 검증 시작");

        // 제목 검증
        validateTitle(request.getTitle());

        // 설명 검증
        validateDescription(request.getDescription());

        // 비밀번호 검증
        validatePassword(request.getPassword());

        // 최대 참가자 수 검증
        validateMaxParticipants(request.getMaxParticipants());

        // 태그 검증
        validateTag(request.getTag());

        // 카테고리 검증
        validateCategory(request.getCategory());

        // 타이머 시간 검증
        validateTimerSettings(request.getFocusTime(), request.getBreakTime());

        log.debug("스터디룸 생성 요청 검증 완료");
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > 1000) {
            throw new StudyRoomValidationException("스터디룸 설명은 1000자 이하여야 합니다.");
        }
        log.debug("설명 검증 통과");
    }

    private void validatePassword(String password) {
        if (password != null && password.length() > 20) {
            throw new StudyRoomValidationException("비밀번호는 20자 이하여야 합니다.");
        }
        log.debug("비밀번호 검증 통과");
    }

    private void validateMaxParticipants(Integer maxParticipants) {
        if (maxParticipants != null) {
            if (maxParticipants < 1) {
                throw new StudyRoomValidationException("최대 참가자 수는 최소 1명이어야 합니다.");
            }
            if (maxParticipants > 6) {
                throw new StudyRoomValidationException("최대 참가자 수는 최대 6명까지 가능합니다.");
            }
        }
        log.debug("최대 참가자 수 검증 통과");
    }

    private void validateTag(String tag) {
        if (tag != null && tag.length() > 50) {
            throw new StudyRoomValidationException("태그는 50자 이하여야 합니다.");
        }
        log.debug("태그 검증 통과");
    }

    private void validateCategory(org.oreo.smore.domain.studyroom.StudyRoomCategory category) {
        if (category == null) {
            throw new StudyRoomValidationException("스터디룸 카테고리는 필수입니다.");
        }
        log.debug("카테고리 검증 통과: [{}]", category);
    }

    private void validateTimerSettings(Integer focusTime, Integer breakTime) {
        // 둘 다 null이면 타이머 설정 없음 (유효함)
        if (focusTime == null && breakTime == null) {
            log.debug("타이머 설정 없음 - 검증 통과");
            return;
        }

        // 하나만 설정된 경우 오류
        if (focusTime == null || breakTime == null) {
            throw new StudyRoomValidationException("집중 시간과 휴식 시간은 함께 설정되어야 합니다.");
        }

        // 집중 시간 범위 검증
        if (focusTime < 5 || focusTime > 240) {
            throw new StudyRoomValidationException("집중 시간은 5분 이상 240분 이하여야 합니다.");
        }

        // 휴식 시간 범위 검증
        if (breakTime < 5 || breakTime > 60) {
            throw new StudyRoomValidationException("휴식 시간은 5분 이상 60분 이하여야 합니다.");
        }

        log.debug("타이머 설정 검증 통과: 집중{}분/휴식{}분", focusTime, breakTime);
    }

    // 초대 해시코드
    private String generateInviteHashCode() {
        String hashCode = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
        log.debug("초대 해시코드 생성: [{}]", hashCode);
        return hashCode;
    }

    private String generateLiveKitRoomId() {
        String liveKitRoomId = "study-room-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8)
                .toLowerCase();
        log.debug("LiveKit 방 ID 생성: [{}]", liveKitRoomId);
        return liveKitRoomId;
    }

    private StudyRoom createStudyRoomEntity(Long userId, CreateStudyRoomRequest request, String inviteHashCode) {
        return StudyRoom.builder()
                .userId(userId)
                .title(trimString(request.getTitle()))
                .description(trimString(request.getDescription()))
                .password(request.hasPassword() ? trimString(request.getPassword()) : null)
                .maxParticipants(getMaxParticipantsWithDefault(request.getMaxParticipants()))
                .tag(trimString(request.getTag()))
                .category(request.getCategory())
                .focusTime(request.getFocusTime())
                .breakTime(request.getBreakTime())
                .inviteHashCode(inviteHashCode)
                .liveKitRoomId(generateLiveKitRoomId())
                .build();
    }

    // 최대 참가자 수 기본값 처리
    private Integer getMaxParticipantsWithDefault(Integer maxParticipants) {
        int defaultValue = maxParticipants != null ? maxParticipants : 6;
        log.debug("최대 참가자 수 결정: 요청값={}, 최종값={}", maxParticipants, defaultValue);
        return defaultValue;
    }

    private String trimString(String str) {
        return str != null ? str.trim() : null;
    }

    private void logRequestDetails(CreateStudyRoomRequest request) {
        log.debug("스터디룸 생성 요청 상세정보 - " +
                        "제목길이: {}자, " +
                        "설명: {}, " +
                        "비밀번호: {}, " +
                        "최대인원: {}명, " +
                        "카테고리: [{}], " +
                        "타이머설정: {}, " +
                        "태그: [{}]",
                request.getTitle() != null ? request.getTitle().length() : 0,
                request.getDescription() != null ? "있음(" + request.getDescription().length() + "자)" : "없음",
                request.hasPassword() ? "설정됨" : "없음",
                getMaxParticipantsWithDefault(request.getMaxParticipants()),
                request.getCategory(),
                request.hasTimerSettings() ?
                        String.format("집중%d분/휴식%d분", request.getFocusTime(), request.getBreakTime()) : "없음",
                request.getTag() != null ? request.getTag() : "없음");
    }

    private void logCreationStatistics(StudyRoom studyRoom) {
        log.info("스터디룸 생성 통계 - " +
                        "방ID: {}, " +
                        "방장ID: {}, " +
                        "카테고리: [{}], " +
                        "비밀방여부: {}, " +
                        "최대인원: {}명, " +
                        "타이머설정: {}, " +
                        "생성시간: {}",
                studyRoom.getRoomId(),
                studyRoom.getUserId(),
                studyRoom.getCategory(),
                studyRoom.getPassword() != null ? "예" : "아니오",
                studyRoom.getMaxParticipants(),
                (studyRoom.getFocusTime() != null && studyRoom.getBreakTime() != null) ?
                        String.format("집중%d분/휴식%d분", studyRoom.getFocusTime(), studyRoom.getBreakTime()) : "없음",
                studyRoom.getCreatedAt());
    }
}
