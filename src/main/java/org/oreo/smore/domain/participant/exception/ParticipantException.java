package org.oreo.smore.domain.participant.exception;

public class ParticipantException extends RuntimeException {
  public ParticipantException(String message) {
    super(message);
  }

  public ParticipantException(String message, Throwable cause) {
    super(message, cause);
  }

  // 참가자를 찾을 수 없는 경우
  public static class ParticipantNotFoundException extends ParticipantException {
    public ParticipantNotFoundException(String message) {
      super(message);
    }

    public ParticipantNotFoundException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  //  스터디룸을 찾을 수 없는 경우
  public static class StudyRoomNotFoundException extends ParticipantException {
    public StudyRoomNotFoundException(String message) {
      super(message);
    }

    public StudyRoomNotFoundException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  // 이미 참가중인 경우
  public static class AlreadyJoinedException extends ParticipantException {
    public AlreadyJoinedException(String message) {
      super(message);
    }

    public AlreadyJoinedException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  // 강퇴된 사용자가 재입장을 시도하는 경우
  public static class BannedUserException extends ParticipantException {
    public BannedUserException(String message) {
      super(message);
    }

    public BannedUserException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  // 방이 가득 찬 경우
  public static class RoomFullException extends ParticipantException {
    public RoomFullException(String message) {
      super(message);
    }

    public RoomFullException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  // 권한이 없는 경우 (방장이 아닌 사용자가 관리 기능 시도)
  public static class UnauthorizedAccessException extends ParticipantException {
    public UnauthorizedAccessException(String message) {
      super(message);
    }

    public UnauthorizedAccessException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  // 참가자 상태 변경 불가능한 경우 (이미 퇴장했거나 강퇴된 사용자)
  public static class InvalidParticipantStateException extends ParticipantException {
    public InvalidParticipantStateException(String message) {
      super(message);
    }

    public InvalidParticipantStateException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  // 방장 자신을 대상으로 하는 잘못된 작업 (방장이 자신을 강퇴하려고 시도 등)
  public static class InvalidOwnerOperationException extends ParticipantException {
    public InvalidOwnerOperationException(String message) {
      super(message);
    }

    public InvalidOwnerOperationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
