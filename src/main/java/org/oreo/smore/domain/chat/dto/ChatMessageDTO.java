package org.oreo.smore.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.oreo.smore.domain.chat.MessageType;

import java.time.LocalDateTime;

public class ChatMessageDTO {

    // 클라 -> 서버로 전송하는 메시지 요청 dto
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {

        @NotNull(message = "룸 ID는 필수입니다")
        private Long roomId;

        @NotBlank(message = "메시지 내용은 필수입니다")
        @Size(max = 500, message = "메시지는 500자를 초과할 수 없습니다")
        private String content;

        @Builder.Default
        private MessageType messageType = MessageType.CHAT;

        // 답글 기능 추가할 수 있음
    }

    // 서버 -> 클라 전송하는 메시지 응답 dto
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {

        private Long messageId;

        @NotNull
        private Long roomId;

        @NotNull
        private Long userId;

        @NotBlank
        private String content;

        @NotNull
        private MessageType messageType;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        // 사용자 정보
        private UserInfo user;

        // 메시지 상태
        @Builder.Default
        private Boolean isEdited = false;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime editedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private Long userId;
        private String nickname;
        private String email;
        private String profileUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Broadcast {

        private Long roomId;
        private Long userId;
        private String nickname;
        private String content;
        private MessageType messageType;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime timestamp;

        // 브로드캐스트 타입 (NEW_MESSAGE, USER_JOIN, USER_LEAVE 등)
        @Builder.Default
        private String broadcastType = "NEW_MESSAGE";

        private Object metadata;
    }
}
