package org.oreo.smore.domain.video.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class OwnerNotJoinedException extends RuntimeException {

    public OwnerNotJoinedException(String message) {
        super(message);
    }

    public OwnerNotJoinedException(String message, Throwable cause) {
        super(message, cause);
    }
}