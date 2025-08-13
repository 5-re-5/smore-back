package org.oreo.smore.domain.chat;

public enum MessageType {
    // 일반 채팅 메시지
    CHAT,

    // 사용자 입장 알림
    USER_JOIN,

    // 사용자 퇴장 알림
    USER_LEAVE,

    // 시스템 메시지
    SYSTEM,

    // 집중 시간 시작 알림
    FOCUS_START,

    // 휴식 시간 시작 알림
    BREAK_START
}
