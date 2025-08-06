package org.oreo.smore.domain.video.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class StudyRoomNotFoundException extends RuntimeException{
    // 스터디룸을 찾을 수 없을 때 발생하는 예외

    public StudyRoomNotFoundException(String message) {
        super(message);
    }

    public StudyRoomNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
