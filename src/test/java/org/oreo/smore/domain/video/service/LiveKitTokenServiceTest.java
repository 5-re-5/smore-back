package org.oreo.smore.domain.video.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.video.dto.TokenRequest;
import org.oreo.smore.domain.video.dto.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class LiveKitTokenServiceTest {

    @Autowired
    private LiveKitTokenService tokenService;

    @Test
    void 실제_서버_토큰_생성_성공() {
        // given
        TokenRequest request = TokenRequest.builder()
                .roomName("실제테스트방")
                .identity("테스트사용자123")
                .canPublish(true)
                .canSubscribe(true)
                .tokenExpirySeconds(1800)
                .build();

        // when
        TokenResponse response = tokenService.generateToken(request);

        // then
        assertNotNull(response, "응답이 null이면 안됩니다");
        assertNotNull(response.getAccessToken(), "실제 액세스 토큰이 생성되어야 합니다");
        assertFalse(response.getAccessToken().isEmpty(), "액세스 토큰이 비어있으면 안됩니다");

        assertEquals("실제테스트방", response.getRoomName());
        assertEquals("테스트사용자123", response.getIdentity());
        assertTrue(response.getCanPublish());
        assertTrue(response.getCanSubscribe());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getExpiresAt());

        // 실제 JWT 토큰 형식 검증
        String[] tokenParts = response.getAccessToken().split("\\.");
        assertEquals(3, tokenParts.length, "실제 JWT 토큰은 3개 부분으로 구성되어야 합니다");

        // 토큰이 실제로 서명되었는지 확인 (각 부분이 비어있지 않음)
        for (int i = 0; i < tokenParts.length; i++) {
            assertFalse(tokenParts[i].isEmpty(),
                    "JWT 토큰의 " + (i + 1) + "번째 부분이 비어있으면 안됩니다");
        }

        log.info("✅ 실제 서버에서 생성된 토큰: {}",
                response.getAccessToken().substring(0, 50) + "...");
    }

    @Test
    void 한글_방이름_토큰_생성() {
        // given
        TokenRequest request = TokenRequest.builder()
                .roomName("한글스터디방")
                .identity("한글사용자")
                .canPublish(true)
                .canSubscribe(true)
                .tokenExpirySeconds(3600)
                .build();

        // when
        TokenResponse response = tokenService.generateToken(request);

        // then
        assertNotNull(response);
        assertEquals("한글스터디방", response.getRoomName());
        assertEquals("한글사용자", response.getIdentity());
        assertNotNull(response.getAccessToken());

        log.info("✅ 한글 방이름으로 토큰 생성 성공: {}", response.getRoomName());
    }

    @Test
    void 기본값_적용_테스트() {
        // given - canSubscribe, tokenExpirySeconds 기본값 사용
        TokenRequest request = TokenRequest.builder()
                .roomName("기본값테스트방")
                .identity("기본값사용자")
                .canPublish(true)
                // canSubscribe, tokenExpirySeconds는 기본값 사용
                .build();

        // when
        TokenResponse response = tokenService.generateToken(request);

        // then
        assertNotNull(response);
        assertTrue(response.getCanSubscribe(), "기본값으로 구독 권한이 true여야 합니다");

        // 기본 만료시간 확인
        LocalDateTime expectedExpiry = LocalDateTime.now().plusSeconds(3600);
        assertTrue(response.getExpiresAt().isAfter(expectedExpiry.minusMinutes(1)));
        assertTrue(response.getExpiresAt().isBefore(expectedExpiry.plusMinutes(1)));

        log.info("✅ 기본값 적용 확인 - 구독권한: {}, 만료시간: {}",
                response.getCanSubscribe(), response.getExpiresAt());
    }

    @Test
    void 토큰_재발급_성공() {
        // given
        String roomName = "재발급테스트방";
        String identity = "재입장사용자";

        // when
        TokenResponse response = tokenService.regenerateToken(roomName, identity);

        // then
        assertNotNull(response);
        assertEquals(roomName, response.getRoomName());
        assertEquals(identity, response.getIdentity());
        assertTrue(response.getCanPublish(), "재발급 시 발행 권한이 기본으로 있어야 합니다");
        assertTrue(response.getCanSubscribe(), "재발급 시 구독 권한이 기본으로 있어야 합니다");
        assertNotNull(response.getAccessToken());

        log.info("✅ 토큰 재발급 성공 - 사용자: {}", identity);
    }

    @Test
    void 여러_사용자_동시_토큰_생성() {
        // given
        TokenRequest request1 = TokenRequest.builder()
                .roomName("다중사용자방")
                .identity("사용자1")
                .canPublish(true)
                .tokenExpirySeconds(1800)
                .build();

        TokenRequest request2 = TokenRequest.builder()
                .roomName("다중사용자방")
                .identity("사용자2")
                .canPublish(true)
                .tokenExpirySeconds(1800)
                .build();

        // when
        TokenResponse response1 = tokenService.generateToken(request1);
        TokenResponse response2 = tokenService.generateToken(request2);

        // then
        assertNotNull(response1);
        assertNotNull(response2);

        // 서로 다른 토큰이어야 함
        assertNotEquals(response1.getAccessToken(), response2.getAccessToken(),
                "각 사용자는 서로 다른 토큰을 가져야 합니다");

        // 같은 방이지만 다른 사용자
        assertEquals("다중사용자방", response1.getRoomName());
        assertEquals("다중사용자방", response2.getRoomName());
        assertEquals("사용자1", response1.getIdentity());
        assertEquals("사용자2", response2.getIdentity());

        log.info("✅ 여러 사용자 토큰 생성 성공 - 방: {}", response1.getRoomName());
    }

    @Test
    void 커스텀_만료시간_토큰_생성() {
        // given
        TokenRequest request = TokenRequest.builder()
                .roomName("커스텀시간방")
                .identity("커스텀사용자")
                .canPublish(true)
                .canSubscribe(true)
                .tokenExpirySeconds(7200) // 2시간
                .build();

        // when
        TokenResponse response = tokenService.generateToken(request);

        // then
        assertNotNull(response);

        // 만료 시간이 약 2시간 후인지 확인
        LocalDateTime expectedExpiry = LocalDateTime.now().plusSeconds(7200);
        assertTrue(response.getExpiresAt().isAfter(expectedExpiry.minusMinutes(1)));
        assertTrue(response.getExpiresAt().isBefore(expectedExpiry.plusMinutes(1)));

        log.info("✅ 커스텀 만료시간 토큰 생성 성공 - 만료시간: {}", response.getExpiresAt());
    }

    @Test
    void 토큰_내용_상세_검증() {
        // given
        TokenRequest request = TokenRequest.builder()
                .roomName("상세검증방")
                .identity("상세검증사용자")
                .canPublish(true)
                .canSubscribe(false)
                .tokenExpirySeconds(1800)
                .build();

        // when
        TokenResponse response = tokenService.generateToken(request);

        // then
        assertNotNull(response.getAccessToken(), "액세스 토큰이 설정되어야 합니다");
        assertEquals("상세검증방", response.getRoomName(), "방 이름이 일치해야 합니다");
        assertEquals("상세검증사용자", response.getIdentity(), "사용자 식별자가 일치해야 합니다");
        assertTrue(response.getCanPublish(), "발행 권한이 일치해야 합니다");
        assertFalse(response.getCanSubscribe(), "구독 권한이 일치해야 합니다");
        assertNotNull(response.getExpiresAt(), "만료시간이 설정되어야 합니다");
        assertNotNull(response.getCreatedAt(), "생성시간이 설정되어야 합니다");

        // 생성시간이 현재 시간 근처인지 확인
        LocalDateTime now = LocalDateTime.now();
        assertTrue(response.getCreatedAt().isBefore(now.plusSeconds(1)));
        assertTrue(response.getCreatedAt().isAfter(now.minusSeconds(10)));

        log.info("✅ 토큰 내용 상세 검증 완료");
    }
}
