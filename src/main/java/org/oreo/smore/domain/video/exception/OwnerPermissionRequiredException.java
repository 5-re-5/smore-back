package org.oreo.smore.domain.video.exception;

public class OwnerPermissionRequiredException extends RuntimeException {
    // 방장 권한이 필요한 작업을 일반 참가자가 시도할 때

    public OwnerPermissionRequiredException(String message) {
        super(message);
    }

    public OwnerPermissionRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
