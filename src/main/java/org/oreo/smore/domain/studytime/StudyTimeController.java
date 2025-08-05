package org.oreo.smore.domain.studytime;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class StudyTimeController {

    private final StudyTimeService studyTimeService;

    /**
     * 공부 시작 API
     * POST /api/v1/study-times/{user_id}
     */
    @PostMapping("/v1/study-times/{userId}")
    public ResponseEntity<String> startStudyTime(@PathVariable Long userId, Authentication authentication) {
        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED); // userId가 다르면 401
        }
        studyTimeService.startStudyTime(userId);
        return ResponseEntity.status(201).body("created");
    }

    @PatchMapping("/v1/study-times/{userId}")
    public ResponseEntity<String> updateStudyTime(@PathVariable Long userId, Authentication authentication) {
        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED); // userId가 다르면 401
        }
        studyTimeService.updateStudyTime(userId);
        return ResponseEntity.ok("OK");
    }
}
