package org.oreo.smore.domain.studyroom.exception;

public class StudyRoomCreationException extends RuntimeException {
    public StudyRoomCreationException(String message) {
        super(message);
    }

    public StudyRoomCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
