package org.oreo.smore.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")             // 모든 엔드포인트에 대해
                        .allowedOriginPatterns("*")              // 모든 origin 허용
                        .allowedMethods("*")                     // 모든 HTTP 메서드 허용
                        .allowedHeaders("*")                     // 모든 요청 헤더 허용
                        .exposedHeaders("Authorization")         // 필요시 노출할 응답 헤더
                        .allowCredentials(true)                  // 쿠키·자격증명 허용
                        .maxAge(3600);                           // pre-flight 캐시 유효기간 (초)
            }
        };
    }
}