package org.oreo.smore.domain.studyroom.service;

import io.openvidu.java.client.OpenVidu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "OPENVIDU_URL=https://i13a505.p.ssafy.io:8443",
        "OPENVIDU_SECRET=jD8fK4qPw1x_2VzLsRm9YeTnA0UcB3zWd7oKiXJ6NvQpGtM5EbChZrLjy"
})
public class VideoSessionServiceIntegrationTest {

    @Test
    @DisplayName("SSL 인증서 검증과 함께 실제 서버 연동 테스트")
    void 실제_서버_SSL_검증_테스트() {
        // given & when
        assertThatCode(() -> {
            // OpenVidu 객체 생성 시 SSL 검증 통과하는지 확인
            OpenVidu testOpenVidu = new OpenVidu(
                    "https://i13a505.p.ssafy.io:8443",
                    "jD8fK4qPw1x_2VzLsRm9YeTnA0UcB3zWd7oKiXJ6NvQpGtM5EbChZrLjy" // 올바른 SECRET 사용
            );

            // 간단한 API 호출로 연결 테스트
            var session = testOpenVidu.createSession();

            // 세션이 정상적으로 생성되었는지 확인
            assertThat(session).isNotNull();
            assertThat(session.getSessionId()).isNotNull();

            System.out.println("📋 생성된 세션 ID: " + session.getSessionId());

        }).doesNotThrowAnyException();

        System.out.println("✅ SSL 검증과 함께 OpenVidu 서버 연결 성공!");
        System.out.println("🌐 대시보드에서 확인: https://i13a505.p.ssafy.io:8443/dashboard");
    }

    @Test
    @DisplayName("여러 세션 생성 테스트")
    void 여러_세션_생성_테스트() {
        // given
        OpenVidu testOpenVidu = new OpenVidu(
                "https://i13a505.p.ssafy.io:8443",
                "jD8fK4qPw1x_2VzLsRm9YeTnA0UcB3zWd7oKiXJ6NvQpGtM5EbChZrLjy"
        );

        // when & then
        assertThatCode(() -> {
            // 3개의 세션을 연속으로 생성
            for (int i = 1; i <= 3; i++) {
                var session = testOpenVidu.createSession();
                System.out.println("📋 세션 " + i + " 생성: " + session.getSessionId());

                assertThat(session.getSessionId()).isNotNull();
            }

        }).doesNotThrowAnyException();

        System.out.println("✅ 여러 세션 생성 테스트 완료!");
    }

    @Test
    @DisplayName("커스텀 세션 ID로 세션 생성 테스트")
    void 커스텀_세션ID_생성_테스트() {
        // given
        OpenVidu testOpenVidu = new OpenVidu(
                "https://i13a505.p.ssafy.io:8443",
                "jD8fK4qPw1x_2VzLsRm9YeTnA0UcB3zWd7oKiXJ6NvQpGtM5EbChZrLjy"
        );

        String customSessionId = "test-session-" + System.currentTimeMillis();

        // when & then
        assertThatCode(() -> {
            var sessionProperties = new io.openvidu.java.client.SessionProperties.Builder()
                    .customSessionId(customSessionId)
                    .build();

            var session = testOpenVidu.createSession(sessionProperties);

            assertThat(session.getSessionId()).contains(customSessionId);
            System.out.println("📋 커스텀 세션 ID: " + session.getSessionId());

        }).doesNotThrowAnyException();

        System.out.println("✅ 커스텀 세션 ID 생성 테스트 완료!");
    }

    @Test
    @DisplayName("생성된 세션 목록 API로 확인")
    void 세션_목록_API_확인() {
        OpenVidu testOpenVidu = new OpenVidu(
                "https://i13a505.p.ssafy.io:8443",
                "jD8fK4qPw1x_2VzLsRm9YeTnA0UcB3zWd7oKiXJ6NvQpGtM5EbChZrLjy"
        );

        try {
            // 1. 현재 활성 세션 수 확인
            var activeSessions = testOpenVidu.getActiveSessions();
            System.out.println("🔍 현재 활성 세션 수: " + activeSessions.size());

            // 2. 새 세션 3개 생성
            String[] sessionIds = new String[3];
            for (int i = 0; i < 3; i++) {
                var session = testOpenVidu.createSession();
                sessionIds[i] = session.getSessionId();
                System.out.println("📋 세션 " + (i+1) + " 생성: " + sessionIds[i]);
            }

            // 3. 다시 활성 세션 수 확인
            var newActiveSessions = testOpenVidu.getActiveSessions();
            System.out.println("🔍 새로운 활성 세션 수: " + newActiveSessions.size());
            System.out.println("✅ 세션 증가: " + (newActiveSessions.size() - activeSessions.size()) + "개");

            // 4. 생성된 세션들이 목록에 있는지 확인
            for (var activeSession : newActiveSessions) {
                String sessionId = activeSession.getSessionId();
                for (String createdId : sessionIds) {
                    if (sessionId.equals(createdId)) {
                        System.out.println("✅ 확인됨: " + sessionId);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ 테스트 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}