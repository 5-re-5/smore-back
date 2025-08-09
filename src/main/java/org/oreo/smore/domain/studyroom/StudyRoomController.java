package org.oreo.smore.domain.studyroom;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.studyroom.dto.*;
import org.oreo.smore.global.common.CursorPage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/study-rooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomCreationService studyRoomCreationService;
    private final StudyRoomService studyRoomService;

    @PostMapping
    public ResponseEntity<CreateStudyRoomResponse> createStudyRoom(
            @RequestParam Long userId,
            @Valid @RequestBody CreateStudyRoomRequest request,
            Authentication authentication
    ) {
        try {
            String principal = authentication.getPrincipal().toString();
            if (!principal.equals(userId.toString())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.error("Authentication validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("스터디룸 생성 API 호출 - 사용자ID: {}, 제목: [{}]", userId, request.getTitle());

        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(userId, request);

        log.info("✅ 스터디룸 생성 API 응답 성공 - 방ID: {}, 사용자ID: {}, 초대코드: [{}]",
                response.getRoomId(), userId, response.getInviteHashCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CursorPage<StudyRoomInfoReadResponse>> listStudyRooms(
            @RequestParam(name="page", defaultValue="1") Long page,
            @RequestParam(name="limit", defaultValue="20") int limit,
            @RequestParam(name="search", required=false) String search,
            @RequestParam(name="category", required=false) String category,
            @RequestParam(name="sort", defaultValue="latest") String sort,
            @RequestParam(name="hideFullRooms", defaultValue="false") boolean hideFullRooms
    ) {
        CursorPage<StudyRoomInfoReadResponse> studyRoomDtoCursorPage = studyRoomService.listStudyRooms(page, limit, search, category, sort, hideFullRooms);
        return ResponseEntity.ok(studyRoomDtoCursorPage);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<StudyRoomDetailResponse> getStudyRoomDetail(@PathVariable Long roomId) {
        return ResponseEntity.ok(studyRoomService.getStudyRoomDetail(roomId));
    }

    @GetMapping("/{userId}/recent-study")
    public ResponseEntity<RecentStudyRoomsResponse> getRecentStudyRooms(@PathVariable Long userId, Authentication authentication) {
        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }
        return ResponseEntity.ok(studyRoomService.getRecentStudyRooms(userId));
    }
}
