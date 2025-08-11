package org.oreo.smore.global.exception;

public class NotRoomOwnerException extends RuntimeException {
    public NotRoomOwnerException(Long roomId, Long userId) {
        super(String.format("방장 권한이 없습니다 - 방ID: %d, 사용자ID: %d", roomId, userId));
    }
}