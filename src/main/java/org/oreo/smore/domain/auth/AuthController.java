package org.oreo.smore.domain.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.auth.jwt.JwtTokenProvider;
import org.oreo.smore.domain.auth.token.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtProvider;
    private final TokenService tokenService;

    @PostMapping("/refresh")
    public ResponseEntity<DataResponse<UserIdDto>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || !jwtProvider.validateToken(refreshToken, false)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        tokenService.checkNotExpired(refreshToken);
        Long userId = tokenService.getUserId(refreshToken);

        // 새 Access Token 세팅
        String newAccess = jwtProvider.createAccessToken(userId.toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("accessToken", newAccess)
                        .httpOnly(true).secure(true)
                        .path("/").maxAge(jwtProvider.getAccessTokenExpMs() / 1000)
                        .sameSite("Lax")
                        .build()
                        .toString()
        );

        // JSON 바디로도 동일 정보 반환
        return ResponseEntity.ok(
                new DataResponse<>( new UserIdDto(userId) )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // ... refreshToken 삭제 로직 생략 ...

        // 쿠키 만료
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("refreshToken", "")
                        .httpOnly(true).secure(true)
                        .path("/").maxAge(0)
                        .sameSite("None")
                        .build()
                        .toString()
        );
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("accessToken", "")
                        .httpOnly(true).secure(true)
                        .path("/").maxAge(0)
                        .sameSite("None")
                        .build()
                        .toString()
        );
        return ResponseEntity.ok().build();
    }

    @Getter
    @AllArgsConstructor
    static class UserIdDto {
        private long userId;
    }

    @Getter
    @AllArgsConstructor
    static class DataResponse<T> {
        private T data;
    }
}
