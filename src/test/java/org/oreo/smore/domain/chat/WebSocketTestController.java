package org.oreo.smore.domain.chat;

import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.user.User;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * WebSocket 테스트용 컨트롤러
 * 실제 채팅 기능 구현 전 연결 및 메시지 송수신 테스트용
 */
@Controller
@Slf4j
public class WebSocketTestController {

    /**
     * Ping-Pong 테스트
     * /app/test/ping -> /topic/test/pong
     */
    @MessageMapping("/test/ping")
    @SendTo("/topic/test/pong")
    public Map<String, Object> handlePing(Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("🏓 Ping 메시지 수신: {}", message);

        // 세션에서 사용자 정보 추출
        User user = (User) headerAccessor.getSessionAttributes().get("user");

        return Map.of(
                "message", "pong",
                "originalMessage", message.get("message"),
                "timestamp", System.currentTimeMillis(),
                "user", Map.of(
                        "userId", user.getUserId(),
                        "nickname", user.getNickname(),
                        "email", user.getEmail()
                )
        );
    }

    /**
     * 상태 확인 테스트
     * /app/test/status -> /topic/test/status
     */
    @MessageMapping("/test/status")
    @SendTo("/topic/test/status")
    public Map<String, Object> handleStatus(Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("📊 상태 확인 요청: {}", message);

        // 세션에서 사용자 정보 추출
        User user = (User) headerAccessor.getSessionAttributes().get("user");

        return Map.of(
                "status", "connected",
                "timestamp", System.currentTimeMillis(),
                "user", Map.of(
                        "userId", user.getUserId(),
                        "nickname", user.getNickname(),
                        "email", user.getEmail()
                )
        );
    }

    /**
     * 개인 메시지 테스트
     * /app/test/private -> /user/queue/test/private
     */
    @MessageMapping("/test/private")
    @SendToUser("/queue/test/private")
    public Map<String, Object> handlePrivateMessage(Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("💌 개인 메시지 수신: {}", message);

        // 세션에서 사용자 정보 추출
        User user = (User) headerAccessor.getSessionAttributes().get("user");

        return Map.of(
                "message", "개인 메시지가 정상적으로 처리되었습니다.",
                "originalMessage", message.get("message"),
                "type", "PRIVATE_RESPONSE",
                "timestamp", System.currentTimeMillis(),
                "user", Map.of(
                        "userId", user.getUserId(),
                        "nickname", user.getNickname(),
                        "email", user.getEmail()
                )
        );
    }

    /**
     * 에코 메시지 테스트 (간단한 응답)
     * /app/test/echo -> /topic/test/echo
     */
    @MessageMapping("/test/echo")
    @SendTo("/topic/test/echo")
    public Map<String, Object> handleEcho(Map<String, Object> message, SimpMessageHeaderAccessor headerAccessor) {
        log.info("🔊 에코 메시지 수신: {}", message);

        return Map.of(
                "echo", message,
                "timestamp", System.currentTimeMillis()
        );
    }
}