package org.oreo.smore.config;
import io.livekit.server.RoomServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class LiveKitConnectionTest {
    @Autowired
    private RoomServiceClient roomServiceClient;

    @Value("${livekit.url}")
    private String liveKitUrl;

    @Value("${livekit.apiKey}")
    private String apiKey;

    @Value("${livekit.apiSecret}")
    private String apiSecret;
}
