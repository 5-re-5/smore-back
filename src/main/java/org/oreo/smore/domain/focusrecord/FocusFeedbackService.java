package org.oreo.smore.domain.focusrecord;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse;
import org.oreo.smore.global.common.GmsProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class FocusFeedbackService {
    private final WebClient client;
    private final GmsProperties props;

    static final List<String> DEFAULT_FEEDBACKS = List.of(
            "훌륭해요! 꾸준히 이어가면 분명 좋은 결과가 있을 거예요😊",
            "오늘도 최선을 다했어요! 내일 더 멋지게 해봐요!",
            "멋져요! 당신의 노력은 꼭 빛을 발할 거예요!",
            "잘하고 있어요! 잠깐의 휴식도 잊지 마세요!",
            "당신의 집중력, 정말 대단해요! 계속 힘내요!",
            "지금까지 잘해왔어요! 앞으로도 파이팅!",
            "한 걸음 한 걸음이 모여 큰 성장을 만듭니다!",
            "집중하는 모습, 정말 멋져요! 계속 이어가요!",
            "당신의 노력에 박수를 보냅니다! 언제나 응원해요!",
            "오늘의 성과도 소중해요! 내일도 기대할게요!"
    );

    public FocusFeedbackService(WebClient.Builder webClientBuilder,
                                GmsProperties props) {
        this.props = props;

        ExchangeFilterFunction requestLogger = ExchangeFilterFunction.ofRequestProcessor(r -> {
            log.info("▶ GMS 피드백 요청 ▶ {} {}", r.method(), r.url());
            return Mono.just(r);
        });
        ExchangeFilterFunction responseLogger = ExchangeFilterFunction.ofResponseProcessor(r -> {
            log.info("◀ GMS 피드백 응답 ◀ {}", r.statusCode());
            return Mono.just(r);
        });

        // endpoint: https://gms.ssafy.io/gmsapi
        this.client = webClientBuilder
                .baseUrl(props.getEndpoint())
                .filter(requestLogger)
                .filter(responseLogger)
                .build();
    }

    public String generateOneLineFeedback(
            FocusRecordsResponse.FocusTimeDto best,
            FocusRecordsResponse.FocusTimeDto worst,
            int avgDurationSeconds,
            FocusRecordsResponse.FocusTrackDto track) {

        String systemPrompt = """
                당신은 심리학 기반의 생산성 코칭 전문가입니다.
                • 제공된 시간 정보는 반복 금지해 주세요.
                • 집중력 ‘급등·급락’ 원인·주의점 1문장,  
                  구체적 실천 팁 또는 응원 1문장(숫자 포함)을 작성합니다.
                • 톤은 더욱 친근하고 부드럽게,  
                  다양한 이모지(😊✨🎉👍🔋💤💡📈)를 활용하세요.
                • 전체를 “최대 2문장”으로 구성하고,  
                  반드시 아래 예시 스타일을 모방합니다.
                
                —— 예시 시작 ——
                집중력이 들쭉날쭉한 건 수면 루틴이 들쭉날쭉해서 그런 것 같아요😴.  
                25분 집중 후 5분 휴식(예: 하루 4회 이상)을 시도해 리듬을 찾아보세요💪
                —— 예시 끝 ——
                """;

        String userPrompt = String.format("""
                        최고 집중 시간대: %s~%s (평균점수: %d)
                        최저 집중 시간대: %s~%s (평균점수: %d)
                        평균 유지 시간: %d초
                        시간대별 집중도: %s
                        위 데이터로부터 “시간 언급 없이”  
                        원인·주의점·실천 팁 또는 응원을  
                        예시 스타일과 같이 친근한 어투 & 다양한 이모지 포함해  
                        최대 2문장으로 작성해 주세요.
                        """,
                best.getStart(), best.getEnd(), best.getAvgFocusScore(),
                worst.getStart(), worst.getEnd(), worst.getAvgFocusScore(),
                avgDurationSeconds,
                track.getScores().toString()
        );


        // max_tokens, temperature 추가
        Map<String, Object> body = Map.of(
                "model", "gpt-4.1-nano",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", 4096,
                "temperature", 0.3
        );

        JsonNode resp;
        try {
            resp = client.post()
                    // proxy 경유 경로를 포함
                    .uri("/api.openai.com/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getKey())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, cr ->
                            cr.bodyToMono(String.class)
                                    .flatMap(err -> {
                                        log.error("GMS 피드백 에러 {}: {}", cr.statusCode(), err);
                                        return Mono.error(new RuntimeException(err));
                                    })
                    )
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("GMS 호출 실패: {} / {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            return getRandomDefaultFeedback();
        } catch (Exception e) { // GMS 키 만료돼서 다 기본 피드백으로 바꿈
            log.error("GMS 호출 중 예외 발생: {}", e.getMessage());
            return getRandomDefaultFeedback();
        }

        return resp.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText()
                .trim();
    }

    private String getRandomDefaultFeedback() {
        int idx = ThreadLocalRandom.current().nextInt(DEFAULT_FEEDBACKS.size());
        return DEFAULT_FEEDBACKS.get(idx);
    }
}
