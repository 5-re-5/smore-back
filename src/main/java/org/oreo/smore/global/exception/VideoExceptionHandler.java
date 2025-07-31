package org.oreo.smore.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import static org.oreo.smore.global.exception.VideoExceptions.*;
@Slf4j
@RestControllerAdvice(basePackages = "org.oreo.smore.domain.video")
public class VideoExceptionHandler {

    // 스터디룸 없음 예외 - 404
    @ExceptionHandler(VideoExceptions.StudyRoomNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStudyRoomNotFound(StudyRoomNotFoundException e) {
        log.warn("[스터디룸 없음] {}", e.getMessage());

        Map<String, Object> response = Map.of(
                "error", "STUDY_ROOM_NOT_FOUND",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 세션 중복 예외 - 409
    @ExceptionHandler(VideoExceptions.SessionAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleSessionAlreadyExists(SessionAlreadyExistsException e) {
        log.warn("[세션 중복] {}", e.getMessage());

        Map<String, Object> response = Map.of(
                "error", "SESSION_ALREADY_EXISTS",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // OpenVidu 관련 예외 - 500
    @ExceptionHandler(VideoExceptions.VideoSessionException.class)
    public ResponseEntity<Map<String, Object>> handleVideoSessionException(VideoSessionException e) {
        log.error("[OpenVidu 오류] {}", e.getMessage(), e);

        Map<String, Object> response = Map.of(
                "error", "VIDEO_SESSION_ERROR",
                "message", "세션 처리 중 오류가 발생했습니다",
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Validation 실패 - 400 (비디오 도메인 전용)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("[Validation 실패] {}", e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = Map.of(
                "error", "VALIDATION_FAILED",
                "message", "입력값 검증에 실패했습니다",
                "fieldErrors", fieldErrors,
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 비디오 도메인 기타 예외 - 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception e) {
        log.error("[비디오 도메인 예상치 못한 오류] {}", e.getMessage(), e);

        Map<String, Object> response = Map.of(
                "error", "INTERNAL_SERVER_ERROR",
                "message", "비디오 세션 처리 중 서버 내부 오류가 발생했습니다",
                "timestamp", LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
