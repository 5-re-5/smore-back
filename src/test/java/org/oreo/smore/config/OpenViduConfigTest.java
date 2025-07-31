package org.oreo.smore.config;

import io.openvidu.java.client.OpenVidu;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "OPENVIDU_URL=https://i13a505.p.ssafy.io:8443",
        "OPENVIDU_SECRET=jD8fK4qPw1x_2VzLsRm9YeTnA0UcB3zWd7oKiXJ6NvQpGtM5EbChZrLjy"
})
class OpenViduConfigTest {

    @Autowired
    private OpenVidu openVidu;

    @Test
    void openViduBean이_정상적으로_생성된다() {
        assertThat(openVidu).isNotNull();
    }

    @Test
    void openViduUrl이_올바른_타입이다() {
        assertThat(openVidu).isInstanceOf(OpenVidu.class);
    }
}
