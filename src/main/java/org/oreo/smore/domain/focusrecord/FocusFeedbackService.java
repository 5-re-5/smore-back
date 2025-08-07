//package org.oreo.smore.domain.focusrecord;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import lombok.RequiredArgsConstructor;
//import org.oreo.smore.domain.focusrecord.dto.FocusTimeDto;
//import org.oreo.smore.domain.focusrecord.dto.FocusTrackDto;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatusCode;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
//import org.springframework.web.reactive.function.client.WebClient;
//import org.springframework.web.reactive.function.client.WebClientResponseException;
//import reactor.core.publisher.Mono;
//
//import java.time.Duration;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ThreadLocalRandom;
//
//@Service
//@RequiredArgsConstructor
//public class FocusFeedbackService {
//    private static final Logger log = LoggerFactory.getLogger(FocusFeedbackService.class);
//
//    private final WebClient client;
//    private final GmsProperties props;
//
//    // 1) 기본 격려 문구 10개
//    private static final List<String> DEFAULT_FEEDBACKS = List.of(
//            "훌륭해요! 꾸준히 이어가면 분명 좋은 결과가 있을 거예요😊",
//            "오늘도 최선을 다했어요! 내일 더 멋지게 해봐요!",
//            "멋져요! 당신의 노력은 꼭 빛을 발할 거예요!",
//            "잘하고 있어요! 잠깐의 휴식도 잊지 마세요!",
//            "당신의 집중력, 정말 대단해요! 계속 힘내요!",
//            "지금까지 잘해왔어요! 앞으로도 파이팅!",
//            "한 걸음 한 걸음이 모여 큰 성장을 만듭니다!",
//            "집중하는 모습, 정말 멋져요! 계속 이어가요!",
//            "당신의 노력에 박수를 보냅니다! 언제나 응원해요!",
//            "오늘의 성과도 소중해요! 내일도 기대할게요!"
//    );
//
//    public FocusFeedbackService(WebClient.Builder webClientBuilder,
//                                GmsProperties props) {
//        this.props = props;
//
//        ExchangeFilterFunction requestLogger = ExchangeFilterFunction.ofRequestProcessor(r -> {
//            log.info("▶ GMS 피드백 요청 ▶ {} {}", r.method(), r.url());
//            return Mono.just(r);
//        });
//        ExchangeFilterFunction responseLogger = ExchangeFilterFunction.ofResponseProcessor(r -> {
//            log.info("◀ GMS 피드백 응답 ◀ {}", r.statusCode());
//            return Mono.just(r);
//        });
//
//        this.client = webClientBuilder
//                .baseUrl(props.getEndpoint())
//                .filter(requestLogger)
//                .filter(responseLogger)
//                .build();
//    }
//
//    /**
//     * best, worst, avgDurationSeconds, track 정보를 보고
//     * 한 줄짜리 피드백을 GMS에 요청해서 받아옵니다.
//     */
//    public String generateOneLineFeedback(FocusTimeDto best,
//                                          FocusTimeDto worst,
//                                          int avgDurationSeconds,
//                                          FocusTrackDto track) {
//        // 1) system prompt: 역할 정의
//        String systemPrompt = """
//            당신은 집중도 분석 데이터를 보고 '딱 한 줄'의 간결한 피드백을 작성하는 AI입니다.
//            - 톤: 따뜻하고 격려하는
//            - 길이: 최대 1문장
//            - 예시) "오전 집중력이 최고니, 이 시간대에 가장 중요한 학습을 배치해 보세요!"
//            """;
//
//        // 2) user prompt: 실제 데이터 삽입
//        String userPrompt = String.format("""
//            최고 집중 시간대: %s~%s (평균점수: %d)
//            최저 집중 시간대: %s~%s (평균점수: %d)
//            평균 집중 유지 시간: %d초
//            시간대별 집중도: %s
//            위 정보를 바탕으로 한 줄 피드백을 작성해 주세요.
//            """,
//                best.getStart(), best.getEnd(), best.getAvgFocusScore(),
//                worst.getStart(), worst.getEnd(), worst.getAvgFocusScore(),
//                avgDurationSeconds,
//                track.getScores().toString()
//        );
//
//        Map<String,Object> body = Map.of(
//                "model", "gpt-4o",
//                "messages", List.of(
//                        Map.of("role", "system",  "content", systemPrompt),
//                        Map.of("role", "user",    "content", userPrompt)
//                )
//        );
//
//        JsonNode resp;
//        try {
//            resp = client.post()
//                    .uri("/v1/chat/completions")
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getKey())
//                    .bodyValue(body)
//                    .retrieve()
//                    .onStatus(HttpStatusCode::isError, cr ->
//                            cr.bodyToMono(String.class)
//                                    .flatMap(errBody -> {
//                                        log.error("GMS 피드백 호출 에러 {}: {}", cr.statusCode(), errBody);
//                                        return Mono.error(new RuntimeException("GMS error: " + errBody));
//                                    })
//                    )
//                    .bodyToMono(JsonNode.class)
//                    .timeout(Duration.ofSeconds(5))
//                    .block();
//        } catch (WebClientResponseException e) {
//            log.error("GMS 피드백 호출 실패: {} / {}", e.getRawStatusCode(), e.getResponseBodyAsString());
//            return getRandomDefaultFeedback();
//        }
//
//        // 3) 결과 리턴
//        return resp
//                .path("choices")
//                .get(0)
//                .path("message")
//                .path("content")
//                .asText()
//                .trim();
//    }
//
//    private String getRandomDefaultFeedback() {
//        int randomIdx = ThreadLocalRandom.current().nextInt(DEFAULT_FEEDBACKS.size());
//        return DEFAULT_FEEDBACKS.get(randomIdx);
//    }
//}
