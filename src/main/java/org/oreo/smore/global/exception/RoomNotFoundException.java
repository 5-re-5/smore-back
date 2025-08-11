package org.oreo.smore.global.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(Long roomId) {
        super("방을 찾을 수 없습니다: " + roomId);
    }
}