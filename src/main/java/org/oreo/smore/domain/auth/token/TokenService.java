package org.oreo.smore.domain.auth.token;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {
    private final TokenRepository tokenRepository;

    @Transactional
    public void deleteByUserId(Long userId) {
        tokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public void deleteRefreshToken(String refreshToken) {
        tokenRepository.findByRefreshToken(refreshToken)
                .ifPresent(tokenRepository::delete);
    }

    @Transactional
    public Token createRefreshToken(Long userId, String refreshToken, Instant expiresAt) {
        Token token = Token.builder()
                .userId(userId)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .build();
        return tokenRepository.save(token);
    }

    @Transactional
    public Token replaceRefreshToken(Long userId, String newRefreshToken, Instant expiresAt) {
        deleteByUserId(userId);
        return createRefreshToken(userId, newRefreshToken, expiresAt);
    }

    @Transactional
    public void checkNotExpired(String refreshToken) {
        Token token = loadTokenEntity(refreshToken);
        if (token.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public Long getUserId(String refreshToken) {
        Token token = loadTokenEntity(refreshToken);
        return token.getUserId();
    }

    private Token loadTokenEntity(String refreshToken) {
        return tokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}