package org.oreo.smore.global.config;
import io.livekit.server.RoomServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testLiveKitConnection() {

        log.info("=== 기본 연결 테스트 ===");

        // RoomServiceClient 생성만 테스트
        assertNotNull(roomServiceClient, "RoomServiceClient가 null입니다");
        log.info("✅ RoomServiceClient Bean 주입 성공!");

        // 설정값 확인
        log.info("설정 확인:");
        log.info("- URL: {}", liveKitUrl);
        log.info("- API Key: {}", apiKey);
        log.info("- API Secret 길이: {}", apiSecret.length());

        log.info("=== 기본 테스트 완료 ===");
    }

    @Test
    void testServerConnectivity() throws IOException {
        // 1) 방 목록 조회 시도 (사이드 이펙트 없는 안전한 호출)
        var response = roomServiceClient.listRooms().execute();

        // 2) HTTP 2xx 응답 확인
        assertTrue(response.isSuccessful(),
                "서버 응답 실패: HTTP " + response.code());

        // 3) 응답 바디가 null이 아닌지 확인
        assertNotNull(response.body(),
                "서버에서 빈 바디를 반환했습니다");

        // 4) 로그로 방 목록 출력 (디버깅용)
        System.out.println("✅ LiveKit 서버 방 목록: " + response.body());
    }
}
