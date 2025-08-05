package org.oreo.smore.domain.video.service;

import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.video.dto.TokenRequest;
import org.oreo.smore.domain.video.dto.TokenResponse;
import org.oreo.smore.domain.video.exception.LiveKitException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class LiveKitTokenService {

    private final String apiKey;
    private final String apiSecret;

    public LiveKitTokenService(@Value("${livekit.apiKey}") String apiKey, @Value("${livekit.apiSecret}") String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    // LiveKit AccessToken 생성
    public TokenResponse generateToken(TokenRequest request) {
        log.info("토큰 생성 요청 → 방: [{}], 사용자: [{}], 발행권한: [{}]",
                request.getRoomName(), request.getIdentity(), request.getCanPublish());
        try {
            // AccessToken 생성
            AccessToken token = new AccessToken(apiKey, apiSecret);

            // 사용자 정보 설정
            token.setName(request.getIdentity());
            token.setIdentity(request.getIdentity());

            // 권한 설정
            token.addGrants(
                    new RoomJoin(true),
                    // 방 지정
                    new RoomName(request.getRoomName())
            );

            // JWT 생성
            String jwt = token.toJwt();

            log.info("✅ 토큰 생성 성공 → 사용자: {}", request.getIdentity());

            return TokenResponse.builder()
                    .accessToken(jwt)
                    .roomName(request.getRoomName())
                    .identity(request.getIdentity())
                    .canPublish(request.getCanPublish())
                    .canSubscribe(request.getCanSubscribe())
                    .expiresAt(LocalDateTime.now().plusSeconds(request.getTokenExpirySeconds()))
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ 토큰 생성 실패 → 방: {}, 사용자: {}, 오류: {}",
                    request.getRoomName(), request.getIdentity(), e.getMessage());
            throw new LiveKitException("토큰 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // 토큰 재발급
    public TokenResponse regenerateToken(String roomName, String identity) {
        log.info("토큰 재발급 요청 → 방: {}, 사용자: {}", roomName, identity);

        TokenRequest request = TokenRequest.builder()
                .roomName(roomName)
                .identity(identity)
                .canPublish(true)
                .canSubscribe(true)
                .tokenExpirySeconds(3600)
                .build();

        return generateToken(request);
    }
}
