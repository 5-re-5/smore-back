package org.oreo.smore.domain.video.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class MaxParticipantsExceededException extends RuntimeException {
    // 스터디룸 최대 인원을 초과했을 때

    public MaxParticipantsExceededException(String message) {
        super(message);
    }

    public MaxParticipantsExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
