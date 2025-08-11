package org.oreo.smore.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 404 - 리소스를 찾을 수 없음
    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<Void> handleRoomNotFound(RoomNotFoundException e) {
        log.error("❌ 방을 찾을 수 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(UserNotParticipantException.class)
    public ResponseEntity<Void> handleUserNotParticipant(UserNotParticipantException e) {
        log.error("❌ 참가자가 아님: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // 401 - 인증 실패
    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<Void> handleIncorrectPassword(IncorrectPasswordException e) {
        log.warn("❌ 비밀번호 불일치: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Void> handleUnauthorized(UnauthorizedException e) {
        log.error("❌ 인증 실패: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // 403 - 권한 없음
    @ExceptionHandler(NotRoomOwnerException.class)
    public ResponseEntity<Void> handleNotRoomOwner(NotRoomOwnerException e) {
        log.error("❌ 방장 권한 없음: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // 400 - 잘못된 요청
    @ExceptionHandler(SelfBanAttemptException.class)
    public ResponseEntity<Void> handleSelfBanAttempt(SelfBanAttemptException e) {
        log.warn("❌ 자기 자신 강퇴 시도: {}", e.getMessage());
        return ResponseEntity.badRequest().build();
    }

    // 409 - 리소스 충돌
    @ExceptionHandler(RoomCapacityExceededException.class)
    public ResponseEntity<Void> handleRoomCapacityExceeded(RoomCapacityExceededException e) {
        log.warn("❌ 방 정원 초과: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}