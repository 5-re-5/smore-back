package org.oreo.smore.global.exception;

public class UserNotParticipantException extends RuntimeException {
    public UserNotParticipantException(Long roomId, Long userId) {
        super(String.format("참가자가 아닙니다 - 방ID: %d, 사용자ID: %d", roomId, userId));
    }
}