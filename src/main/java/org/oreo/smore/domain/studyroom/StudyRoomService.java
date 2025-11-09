package org.oreo.smore.domain.studyroom;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.chat.ChatRoomService;
import org.oreo.smore.domain.chat.StudyRoomNotificationService;
import org.oreo.smore.domain.participant.Participant;
import org.oreo.smore.domain.participant.ParticipantRepository;
import org.oreo.smore.domain.studyroom.dto.RecentStudyRoomsResponse;
import org.oreo.smore.domain.studyroom.dto.StudyRoomDetailResponse;
import org.oreo.smore.domain.studyroom.dto.StudyRoomInfoReadResponse;
import org.oreo.smore.domain.participant.ParticipantService;
import org.oreo.smore.domain.video.service.LiveKitRoomService;
import org.oreo.smore.global.common.CursorPage;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyRoomService {
    private final StudyRoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepo;
    private final ParticipantService participantService;
    private final LiveKitRoomService liveKitRoomService;
    private final ChatRoomService chatRoomService;
    private final StudyRoomNotificationService notificationService;

    // TODO : N+1 문제 해결하기
    public CursorPage<StudyRoomInfoReadResponse> listStudyRooms(
            Long page,
            int limit,
            String search,
            String category,
            String sort,
            boolean hideFullRooms
    ) {
        long cursor = (page != null && page > 1) ? page : Long.MAX_VALUE;
        ;
        Sort sortOrder = buildSortOrder(sort);
        Pageable pageable = PageRequest.of(0, limit + 1, sortOrder);

        List<StudyRoom> rooms = fetchRooms(cursor, search, category, pageable);
        List<StudyRoomInfoReadResponse> dtos = mapAndFilterRooms(rooms, hideFullRooms);
        if (isPopularSort(sort)) {
            applyPopularSort(dtos);
        }
        return CursorPage.of(dtos, limit);
    }

    private Sort buildSortOrder(String sort) {
        if (isPopularSort(sort)) {
            // 키셋 페이징을 위해 일관된 정렬 컬럼(실존 필드) 사용
            return Sort.by(Sort.Direction.DESC, "roomId");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private boolean isPopularSort(String sort) {
        return "popular".equalsIgnoreCase(sort);
    }

    private List<StudyRoom> fetchRooms(
            long cursor,
            String search,
            String category,
            Pageable pageable
    ) {
        if ((search == null || search.isBlank()) &&
                (category == null || category.isBlank())) {
            // 필터 없을 때는 Fetch Join 사용
            return roomRepository.findAllWithUserFetchJoin(cursor, pageable);
        }

        Specification<StudyRoom> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.lessThan(root.get("roomId"), cursor));
            addSearchPredicate(cb, root, predicates, search);
            addCategoryPredicate(cb, root, predicates, category);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return roomRepository.findAll(spec, pageable).getContent();
    }

    private void addSearchPredicate(
            CriteriaBuilder cb,
            Root<StudyRoom> root,
            List<Predicate> preds,
            String search
    ) {
        if (search != null && !search.isBlank()) {
            preds.add(cb.like(
                    cb.lower(root.get("title")),
                    "%" + search.toLowerCase() + "%"
            ));
        }
    }

    private void addCategoryPredicate(
            CriteriaBuilder cb,
            Root<StudyRoom> root,
            List<Predicate> preds,
            String category
    ) {
        if (category != null && !category.isBlank()) {
            try {
                StudyRoomCategory catEnum = StudyRoomCategory.valueOf(category.toUpperCase());
                preds.add(cb.equal(root.get("category"), catEnum));
            } catch (IllegalArgumentException e) {
                preds.add(cb.disjunction());
            }
        }
    }

    private List<StudyRoomInfoReadResponse> mapAndFilterRooms(
            List<StudyRoom> rooms,
            boolean hideFullRooms
    ) {

        // 모든 방 ID 수집
        List<Long> roomIds = rooms.stream()
                .map(StudyRoom::getRoomId)
                .collect(Collectors.toList());

        if (roomIds.isEmpty()) {
            return List.of();
        }

        // Bulk Query로 참가자 수 한 번에 조회
        Map<Long, Long> participantCountMap = participantRepository
                .countActiveParticipantsByRoomIds(roomIds)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));

        return rooms.stream()
                .map(room -> toDto(room, participantCountMap))
                .filter(dto -> !hideFullRooms || dto.getCurrentParticipants() < dto.getMaxParticipants())
                .collect(Collectors.toList());
    }

    private StudyRoomInfoReadResponse toDto(StudyRoom room, Map<Long, Long> participantCountMap) {
        // Map에서 참가자 수 꺼내기 -> 없으면 0
        long count = participantCountMap.getOrDefault(room.getRoomId(), 0L);

        User creator = room.getUser();

        String creatorNickname = (creator != null) ? creator.getNickname() : "Unknown";

        return StudyRoomInfoReadResponse.of(room, count, creatorNickname);
//        User creator = userRepo.findById(room.getUserId())
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.NOT_FOUND,
//                        "Creator not found: " + room.getUserId()
//                ));
//        return StudyRoomInfoReadResponse.of(room, count, creator.getNickname());
    }

    private void applyPopularSort(List<StudyRoomInfoReadResponse> dtos) {
        dtos.sort(
                Comparator.comparingLong(StudyRoomInfoReadResponse::getCurrentParticipants).reversed()
        );
    }

    // 방 삭제
    @Transactional
    public void deleteStudyRoom(Long roomId, Long ownerId) {
        log.warn("방 삭제 처리 시작 - 방ID: {}, 방장ID: {}", roomId, ownerId);

        try {
            // 1. 방장 권한 확인
            validateRoomOwner(roomId, ownerId);

            // 2. 현재 참가자 수 확인
            List<Participant> activeParticipants = participantService.getActiveParticipants(roomId);
            long participantCount = activeParticipants.size();

            log.info("방 삭제 전 상태 - 방ID: {}, 활성 참가자 수: {}명", roomId, participantCount);

            if (participantCount > 1) {
                log.warn("⚠️ 다른 참가자가 있는 상태에서 방 삭제 - 방ID: {}, 영향받는 참가자: {}명",
                        roomId, participantCount - 1);
            }

            // 3. 참가 이력 삭제
            participantService.deleteAllParticipantsByRoom(roomId);
            log.info("✅ 참가 이력 삭제 완료 - 방ID: {}", roomId);

            // 4. ✅ ChatRoom 및 메시지 삭제 추가
            try {
                chatRoomService.deleteChatRoomByStudyRoom(roomId);
                log.info("✅ 채팅방 및 메시지 삭제 완료 - 방ID: {}", roomId);
            } catch (Exception e) {
                log.error("❌ 채팅방 삭제 실패 (계속 진행) - 방ID: {}, 오류: {}", roomId, e.getMessage());
            }

            // 5. StudyRoom 소프트 삭제
            StudyRoom room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));

            room.delete();
            roomRepository.save(room);
            log.info("✅ 스터디룸 삭제 완료 - 방ID: {}", roomId);

            // 6. LiveKit 방 삭제
            try {
                String roomName = LiveKitRoomService.generateRoomName(roomId);
                liveKitRoomService.deleteRoomSafely(roomName);
                log.info("✅ LiveKit 방 삭제 완료 - 방ID: {}, LiveKit방명: {}", roomId, roomName);
            } catch (Exception e) {
                log.error("❌ LiveKit 방 삭제 실패 (무시됨) - 방ID: {}, 오류: {}", roomId, e.getMessage());
            }

            log.warn("✅ 방 삭제 완료 - 방ID: {}, 방장ID: {}, 삭제된 참가자 수: {}명",
                    roomId, ownerId, participantCount);

        } catch (Exception e) {
            log.error("❌ 방 삭제 실패 - 방ID: {}, 방장ID: {}, 오류: {}", roomId, ownerId, e.getMessage(), e);
            throw new RuntimeException("방 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private void validateRoomOwner(Long roomId, Long ownerId) {

        StudyRoom studyRoom = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 방 - 방ID: {}", roomId);
                    return new IllegalArgumentException("방을 찾을 수 없습니다: " + roomId);
                });

        if (!studyRoom.getUserId().equals(ownerId)) {
            log.error("방장 권한 없음 - 방ID: {}, 요청자: {}, 실제방장: {}",
                    roomId, ownerId, studyRoom.getUserId());
            throw new IllegalArgumentException("방장만 방을 삭제할 수 있습니다");
        }

        log.debug("✅ 방장 권한 확인 완료 - 방ID: {}, 방장ID: {}", roomId, ownerId);
    }

    // 방장 퇴장으로 인한 방 삭제
    @Transactional
    public void deleteStudyRoomByOwnerLeave(Long roomId, Long ownerId) {
        log.warn("방장 퇴장으로 인한 방 삭제 처리 - 방ID: {}, 방장ID: {}", roomId, ownerId);

        try {
            // 현재 참가자 상황 확인
            long participantCount = participantService.getActiveParticipantCount(roomId);
            log.info("방 삭제 전 참가자 수 - 방ID: {}, 참가자: {}명", roomId, participantCount);

            if (participantCount > 1) {
                log.warn("⚠️ 다른 참가자 {}명이 강제 퇴장됨 - 방ID: {}", participantCount - 1, roomId);

                // TODO: WebSocket으로 다른 참가자들에게 방 삭제 알림
                notificationService.notifyRoomDeleted(roomId, "OWNER_LEFT");
            }

            // 모든 참가 이력 삭제
            participantService.deleteAllParticipantsByRoom(roomId);
            log.info("✅ 참가 이력 삭제 완료 - 방ID: {}", roomId);

            // ChatRoom 및 메시지 삭제 추가
            try {
                chatRoomService.deleteChatRoomByStudyRoom(roomId);
                log.info("✅ 채팅방 및 메시지 삭제 완료 - 방ID: {}", roomId);
            } catch (Exception e) {
                log.error("❌ 채팅방 삭제 실패 (계속 진행) - 방ID: {}, 오류: {}", roomId, e.getMessage());
            }

            // 스터디룸 삭제
            StudyRoom room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다: " + roomId));

            room.delete();
            roomRepository.save(room);
            log.info("✅ 스터디룸 소프트 삭제 완료 - 방ID: {}", roomId);

            // 5. LiveKit 방 삭제
            try {
                String roomName = LiveKitRoomService.generateRoomName(roomId);
                liveKitRoomService.deleteRoomSafely(roomName);
                log.info("✅ LiveKit 방 삭제 완료 - 방ID: {}, LiveKit방명: {}", roomId, roomName);
            } catch (Exception liveKitError) {
                log.error("❌ LiveKit 방 삭제 실패 (무시됨) - 방ID: {}, 오류: {}", roomId, liveKitError.getMessage());
            }

            log.warn("✅ 방장 퇴장으로 방 완전 삭제 완료 - 방ID: {}, 방장ID: {}, 총 영향받은 참가자: {}명",
                    roomId, ownerId, participantCount);

        } catch (Exception e) {
            log.error("❌ 방장 퇴장으로 인한 방 삭제 실패 - 방ID: {}, 방장ID: {}, 오류: {}",
                    roomId, ownerId, e.getMessage(), e);
            throw new RuntimeException("방 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    public StudyRoomDetailResponse getStudyRoomDetail(Long roomId) {
        // 1. 스터디룸 조회
        StudyRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스터디룸이 존재하지 않습니다."));

        // 2. 생성자 정보 조회
        User creator = userRepo.findById(room.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스터디룸 생성자가 존재하지 않습니다."));

        // 3. 현재 참가자 수 계산 (퇴장 안 했고 강퇴도 안 당한 사용자)
        int currentParticipants = (int) participantRepository.countActiveParticipantsByRoomId(roomId);

        // 4. 비밀번호 존재 여부 판단
        boolean hasPassword = room.getPassword() != null && !room.getPassword().isBlank();

        // 4. 응답 DTO 생성
        return StudyRoomDetailResponse.builder()
                .roomId(room.getRoomId())
                .title(room.getTitle())
                .description(room.getDescription())
                .thumbnailUrl(room.getThumbnailUrl())
                .tag(room.getTag())
                .category(room.getCategory().getValue())
                .focusTime(room.getFocusTime())
                .breakTime(room.getBreakTime())
                .maxParticipants(room.getMaxParticipants())
                .currentParticipants(currentParticipants)
                .password(hasPassword)
                .createdAt(room.getCreatedAt().toString())
                .creator(StudyRoomDetailResponse.CreatorDto.builder()
                        .userId(creator.getUserId())
                        .nickname(creator.getNickname())
                        .build())
                .build();

    }

    public RecentStudyRoomsResponse getRecentStudyRooms(Long userId) {
        // 1. 유저의 참여 기록 중 최신 참여 기준으로 정렬
        List<Participant> recentParticipants = participantRepository
                .findByUserIdOrderByJoinedAtDesc(userId);

        // 2. 중복 없는 roomId 3개 추출 (최신순 유지)
        LinkedHashSet<Long> distinctRoomIds = new LinkedHashSet<>();
        for (Participant p : recentParticipants) {
            distinctRoomIds.add(p.getRoomId());
            if (distinctRoomIds.size() == 3) break;
        }

        List<RecentStudyRoomsResponse.RoomDto> rooms = distinctRoomIds.stream()
                .map(roomId -> {
                    StudyRoom room = roomRepository.findById(roomId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "스터디룸이 존재하지 않습니다."));

                    User owner = userRepo.findById(room.getUserId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "방장이 존재하지 않습니다."));

                    int currentParticipants = (int) participantRepository.countActiveParticipantsByRoomId(roomId);

                    boolean hasPassword = room.getPassword() != null && !room.getPassword().isBlank();

                    return RecentStudyRoomsResponse.RoomDto.builder()
                            .roomId(room.getRoomId())
                            .title(room.getTitle())
                            .owner(owner.getNickname())
                            .category(room.getCategory().getValue())
                            .maxParticipants(room.getMaxParticipants())
                            .currentParticipants(currentParticipants)
                            .password(hasPassword)
                            .tag(room.getTag())
                            .thumbnailUrl(room.getThumbnailUrl())
                            .isDelete(room.getDeletedAt() != null)
                            .build();
                })
                .toList();

        return RecentStudyRoomsResponse.builder()
                .rooms(rooms)
                .build();
    }

}
