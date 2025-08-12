package org.oreo.smore.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.global.websocket.ChatChannelInterceptor;
import org.oreo.smore.global.websocket.ChatHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatHandshakeInterceptor chatHandshakeInterceptor;
    private final ChatChannelInterceptor chatChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 prefix 설정
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 메시지를 보낼 때 사용할 prefix 설정
        config.setApplicationDestinationPrefixes("/app");

        // 사용자별 개인 메시지 prefix 설정
        config.setUserDestinationPrefix("/user");

        log.info("✅ STOMP 메시지 브로커 설정 완료");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 등록
        registry.addEndpoint("/ws/chat")
                .addInterceptors(chatHandshakeInterceptor)
                .setAllowedOriginPatterns("*") // 개발 환경용, 프로덕션에서는 도메인 지정
                .withSockJS(); // SockJS 폴백 지원

        registry.addEndpoint("/ws/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(chatHandshakeInterceptor);

        log.info("✅ WebSocket 엔드포인트 등록 완료: /ws/chat");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 클라이언트 → 서버 메시지 인터셉터 등록
        registration.interceptors(chatChannelInterceptor);
        log.info("✅ 인바운드 채널 인터셉터 등록 완료");
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        // MappingJackson2MessageConverter 생성
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();

        // ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();

        // JSR310 모듈 등록 (LocalDateTime 등 Java 8 시간 타입 지원)
        objectMapper.registerModule(new JavaTimeModule());

        // 날짜를 timestamp가 아닌 ISO 형식으로 직렬화
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 컨버터에 ObjectMapper 설정
        converter.setObjectMapper(objectMapper);

        // 메시지 컨버터 리스트에 추가
        messageConverters.add(converter);

        log.info("✅ WebSocket 메시지 컨버터 설정 완료 - JSR310 모듈 등록");

        return false; // 기본 컨버터 사용하지 않음
    }
}
