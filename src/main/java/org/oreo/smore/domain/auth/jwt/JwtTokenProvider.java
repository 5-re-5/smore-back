package org.oreo.smore.domain.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.access-token-secret}")
    private String accessSecret;             // Base64로 인코딩된 시크릿
    @Value("${jwt.refresh-token-secret}")
    private String refreshSecret;            // Base64로 인코딩된 시크릿
    @Getter
    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpMs;
    @Getter
    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpMs;

    // Secret(String) → Key 변환 헬퍼
    private Key toKey(String base64Secret) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Access Token 생성
    public String createAccessToken(String userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpMs);
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(toKey(accessSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(String userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpMs);
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(toKey(refreshSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 userId 추출
    public String getUserIdFromToken(String token, boolean isAccess) {
        Key key = isAccess ? toKey(accessSecret) : toKey(refreshSecret);
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 유효성 검증
    public boolean validateToken(String token, boolean isAccess) {
        try {
            Key key = isAccess ? toKey(accessSecret) : toKey(refreshSecret);

            Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
