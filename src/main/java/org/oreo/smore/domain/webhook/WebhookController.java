package org.oreo.smore.domain.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class WebhookController {

    @PostMapping("/v1/webhook")
    public ResponseEntity<Void> handle(@RequestBody Map<String, Object> payload) throws Exception {
        System.out.println(new ObjectMapper().writeValueAsString(payload));
        return ResponseEntity.ok().build();
    }
}
