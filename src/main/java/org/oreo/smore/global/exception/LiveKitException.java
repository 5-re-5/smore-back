package org.oreo.smore.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class LiveKitException extends RuntimeException {

    public LiveKitException(){
        super("LiveKit 서버 통신 중 오류가 발생했습니다.");
    }

    public LiveKitException(String message) {
        super(message);
    }

    public LiveKitException(String message, Throwable cause) {
        super(message, cause);
    }

    public LiveKitException(Throwable cause) {
        super("LiveKit 서버 통신 중 오류가 발생했습니다.", cause);
    }
}
