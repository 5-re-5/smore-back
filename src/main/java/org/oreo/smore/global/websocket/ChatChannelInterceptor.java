package org.oreo.smore.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatChannelInterceptor implements ChannelInterceptor {

    public final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // STOMP 명령어별 처리
        switch (command) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor);
            case SEND -> handleSend(accessor);
            case DISCONNECT -> handleDisconnect(accessor);
            default -> {
                // 기타 명령어는 로깅만
                log.debug("STOMP 명령어: {}, 사용자: {}", command, getUserInfo(accessor));
            }
        }

        return message;
    }

    // STOMP CONNECT 처리
    private void handleConnect(StompHeaderAccessor accessor) {
        // WebSocket 세션에서 사용자 정보 가져오기
        Long userId = (Long) accessor.getSessionAttributes().get("userId");
        String userEmail = (String) accessor.getSessionAttributes().get("userEmail");

        if (userId == null || userEmail == null) {
            log.warn("❌ STOMP CONNECT 실패: 사용자 정보 없음 - 세션: {}", accessor.getSessionId());
            return;
        }

        try {
            // 사용자 정보 조회 및 Principal 설정
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

            // Spring Security Principal 설정
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    user.getEmail(),
                    null,
                    List.of()
            );
            accessor.setUser(auth);

            // 세션에 사용자 정보 저장
            accessor.getSessionAttributes().put("user", user);

            log.info("✅ STOMP CONNECT 성공 - 사용자: {} (ID: {}), 세션: {}",
                    user.getNickname(), userId, accessor.getSessionId());

        } catch (Exception e) {
            log.error("❌ STOMP CONNECT 중 오류 - 사용자ID: {}, 오류: {}", userId, e.getMessage());
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String userInfo = getUserInfo(accessor);

        if (destination != null && destination.startsWith("/topic/chat/")) {
            String roomId = destination.substring("/topic/chat/".length());
            log.info("📥 채팅방 구독 - 방: {}, 사용자: {}, 세션: {}",
                    roomId, userInfo, accessor.getSessionId());

            // TODO: 채팅방 구독 권한 검증 로직 추가 (방 참가자 확인)
        }
    }

    // STOMP SEND 처리 (메시지 전송)
    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String userInfo = getUserInfo(accessor);

        if (destination != null && destination.startsWith("/app/chat/")) {
            log.debug("📤 메시지 전송 요청 - 목적지: {}, 사용자: {}", destination, userInfo);

            // 사용자 정보를 헤더에 추가 (ChatController에서 사용)
            User user = (User) accessor.getSessionAttributes().get("user");
            if (user != null) {
                accessor.setHeader("userId", user.getUserId());
                accessor.setHeader("userNickname", user.getNickname());
                accessor.setHeader("userProfileUrl", user.getProfileUrl());
            }
        }
    }

    // STOMP DISCONNECT 처리
    private void handleDisconnect(StompHeaderAccessor accessor) {
        String userInfo = getUserInfo(accessor);
        log.info("🔌 STOMP 연결 해제 - 사용자: {}, 세션: {}", userInfo, accessor.getSessionId());
    }

    private String getUserInfo(StompHeaderAccessor accessor) {
        User user = (User) accessor.getSessionAttributes().get("user");
        if (user != null) {
            return String.format("%s(ID:%d)", user.getNickname(), user.getUserId());
        }

        Long userId = (Long) accessor.getSessionAttributes().get("userId");
        String userEmail = (String) accessor.getSessionAttributes().get("userEmail");

        if (userId != null && userEmail != null) {
            return String.format("%s(ID:%d)", userEmail, userId);
        }

        return "익명";
    }

}
