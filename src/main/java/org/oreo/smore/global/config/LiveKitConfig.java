package org.oreo.smore.global.config;

import io.livekit.server.RoomServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class LiveKitConfig {

    @Value("${livekit.url}")
    private String liveKitUrl;

    @Value("${livekit.apiKey}")
    private String apiKey;

    @Value("${livekit.apiSecret}")
    private String apiSecret;

    @Bean
    public RoomServiceClient roomServiceClient() {
        log.info("OpenVidu3 서버 연결 설정 - URL: {}, API Key: {}", liveKitUrl, apiKey);

        return RoomServiceClient.create(
                liveKitUrl,
                apiKey,
                apiSecret
        );
    }
}
