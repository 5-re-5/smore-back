package org.oreo.smore.domain.point;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.point.dto.response.TotalPointsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PointController {
    private final PointService pointService;

    @GetMapping("/v1/points/{userId}")
    public ResponseEntity<TotalPointsResponse> getTotalPoints(@PathVariable Long userId, Authentication authentication) {
        if (Long.parseLong(authentication.getPrincipal().toString()) != userId) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN); // userId가 다르면 403
        }
        return ResponseEntity.ok(pointService.getTotalPoints(userId));
    }
}
