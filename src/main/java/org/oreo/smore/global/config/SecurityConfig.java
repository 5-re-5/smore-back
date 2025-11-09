package org.oreo.smore.global.config;

import org.oreo.smore.domain.auth.jwt.JwtAuthenticationFilter;
import org.oreo.smore.domain.auth.jwt.JwtTokenProvider;
import org.oreo.smore.domain.auth.oauth.CustomOAuth2UserService;
import org.oreo.smore.domain.auth.oauth.OAuth2SuccessHandler;
import org.oreo.smore.domain.auth.token.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider tokenProvider;
    private final TokenService tokenService;
    private final CustomOAuth2UserService oAuth2UserService;

    @Value("${app.oauth2.frontend.success-redirect-url}")
    private String successRedirectUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) //cors 먼저 적용
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
//                                "/v1/auth/**",                  // 자체 로그인/회원가입 API
//                                "/oauth2/authorization/**",     // OAuth2 로그인 진입점
//                                "/login/oauth2/code/**",         // OAuth2 콜백 엔드포인트
//                                "/v1/webhook/**",
//                                "/actuator/**",
//                                // 테스트 기간 동안만 추가
//                                "/v1/study-rooms/**",
//                                "/v1/focus-records/**",
//                                "/v1/users/**",
//                                "/v1/points/**",
//                                "/v1/study-times/**"
                                "/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authz -> authz
                                .baseUri("/oauth2/authorization")
                        )
                        .redirectionEndpoint(redir -> redir
                                .baseUri("/login/oauth2/code/*")
                        )
                        .userInfoEndpoint(u -> u.userService(oAuth2UserService))
                        .successHandler(new OAuth2SuccessHandler(tokenProvider, tokenService, successRedirectUrl))
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(tokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }
}
