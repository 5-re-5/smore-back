package org.oreo.smore.domain.chat;

import org.mockito.Mockito;
import org.oreo.smore.domain.video.service.LiveKitRoomService;
import org.oreo.smore.global.common.GmsProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * WebSocket 테스트용 설정
 * @MockBean 대신 @TestConfiguration 사용
 */
@TestConfiguration
@Profile("test")
public class WebSocketTestConfig {

    /**
     * LiveKitRoomService Mock 빈
     */
    @Bean
    @Primary
    public LiveKitRoomService liveKitRoomService() {
        return Mockito.mock(LiveKitRoomService.class);
    }

}