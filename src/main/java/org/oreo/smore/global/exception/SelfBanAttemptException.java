package org.oreo.smore.global.exception;

public class SelfBanAttemptException extends RuntimeException {
    public SelfBanAttemptException(Long roomId, Long userId) {
        super(String.format("자기 자신을 강퇴할 수 없습니다 - 방ID: %d, 사용자ID: %d", roomId, userId));
    }
}