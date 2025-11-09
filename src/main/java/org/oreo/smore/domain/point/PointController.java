package org.oreo.smore.domain.point;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.point.dto.response.TotalPointsResponse;
import org.oreo.smore.domain.point.dto.response.OreoDrawResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PointController {
    private final PointService pointService;

    @GetMapping("/v1/points/{userId}")
    public ResponseEntity<TotalPointsResponse> getTotalPoints(@PathVariable Long userId, Authentication authentication) {
//        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
//            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
//        }
        if (authentication != null && Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }
        return ResponseEntity.ok(pointService.getTotalPoints(userId));
    }

    @PostMapping("/v1/points/{userId}")
    public ResponseEntity<OreoDrawResponse> drawOreo(@PathVariable Long userId, Authentication authentication) {
//        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
//            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
//        }
        if (authentication != null && Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }
        return ResponseEntity.ok(pointService.drawOreo(userId));
    }
}
