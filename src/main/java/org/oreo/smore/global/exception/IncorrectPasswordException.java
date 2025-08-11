package org.oreo.smore.global.exception;

public class IncorrectPasswordException extends RuntimeException {
    public IncorrectPasswordException(Long roomId) {
        super("비밀번호가 틀렸습니다 - 방ID: " + roomId);
    }
}