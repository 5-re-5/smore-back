package org.oreo.smore.domain.studyroom.exception;

public class StudyRoomValidationException extends RuntimeException {
    public StudyRoomValidationException(String message) {
        super(message);
    }

    public StudyRoomValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
