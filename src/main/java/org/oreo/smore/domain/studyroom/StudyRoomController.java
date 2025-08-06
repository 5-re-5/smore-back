package org.oreo.smore.domain.studyroom;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomRequest;
import org.oreo.smore.domain.studyroom.dto.CreateStudyRoomResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/study-rooms")
@RequiredArgsConstructor
public class StudyRoomController {

    private final StudyRoomCreationService studyRoomCreationService;

    @PostMapping
    public ResponseEntity<CreateStudyRoomResponse> createStudyRoom(
            @RequestParam Long userId,
            @Valid @RequestBody CreateStudyRoomRequest request
    ) {
        log.info("스터디룸 생성 API 호출 - 사용자ID: {}, 제목: [{}]", userId, request.getTitle());

        CreateStudyRoomResponse response = studyRoomCreationService.createStudyRoom(userId, request);

        log.info("✅ 스터디룸 생성 API 응답 성공 - 방ID: {}, 사용자ID: {}, 초대코드: [{}]",
                response.getRoomId(), userId, response.getInviteHashCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
