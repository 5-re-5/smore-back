package org.oreo.smore.domain.studytime;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.studytime.dto.response.StudyTimeStatisticsResponse;
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
//        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
//            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
//        }
        if (authentication != null && Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }
        studyTimeService.startStudyTime(userId);
        return ResponseEntity.status(201).body("created");
    }

    @PatchMapping("/v1/study-times/{userId}")
    public ResponseEntity<String> updateStudyTime(@PathVariable Long userId, Authentication authentication) {
//        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
//            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
//        }
        if (authentication != null && Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }
        studyTimeService.updateStudyTime(userId);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/v1/study-times/statistics/{userId}")
    public ResponseEntity<StudyTimeStatisticsResponse> getStatistics(
            @PathVariable Long userId,
            Authentication authentication
    ) {
//        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
//            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
//        }
        if (authentication != null && Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }
        return ResponseEntity.ok(studyTimeService.getStatistics(userId));
    }
}
