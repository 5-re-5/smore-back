package org.oreo.smore.domain.focusrecord;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/focus-records")
@RequiredArgsConstructor
public class FocusRecordController {

    private final FocusRecordService focusRecordService;

    @GetMapping("/{userId}")
    public ResponseEntity<Object> getFocusRecords(
            @PathVariable Long userId,
            @RequestParam(name = "tz", required = false, defaultValue = "+09:00") String tzOffset,
            Authentication authentication
    ) {
        // 성능 테스트 시 인증 우회 (authentication이 null일 경우)
        if (authentication != null && Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }

        FocusRecordsResponse response = focusRecordService.getFocusRecords(userId, tzOffset);
        return ResponseEntity.ok(response);
    }
}
