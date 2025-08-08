package org.oreo.smore.global.config;

import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.global.common.GmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class WebClientConfig {

    /**
     * WebClient.Builder 빈 등록.
     * 이 Builder를 주입받아, FocusFeedbackService 에서
     * .baseUrl(), .filter() 등을 설정할 수 있습니다.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * (선택) GMS 전용 WebClient 빈 등록.
     * GmsProperties 를 주입받아 baseUrl 과 로깅 필터를 미리 설정해두고 싶다면
     * 아래처럼 WebClient 빈을 직접 정의해도 됩니다.
     */
    @Bean
    public WebClient gmsWebClient(WebClient.Builder builder, GmsProperties props) {
        return builder
                .baseUrl(props.getEndpoint())
                .filter(ExchangeFilterFunction.ofRequestProcessor(req -> {
                    log.info("▶ GMS 요청 ▶ {} {}", req.method(), req.url());
                    return Mono.just(req);
                }))
                .filter(ExchangeFilterFunction.ofResponseProcessor(res -> {
                    log.info("◀ GMS 응답 ◀ {}", res.statusCode());
                    return Mono.just(res);
                }))
                .build();
    }
}
