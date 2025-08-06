package org.oreo.smore.domain.video.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenRequestTest {

    @Test
    void testBuilderWithDefaults() {
        // given & when
        TokenRequest request = TokenRequest.builder()
                .roomName("test-room")
                .identity("user123")
                .canPublish(true)
                .build();

        // then
        assertEquals("test-room", request.getRoomName());
        assertEquals("user123", request.getIdentity());
        assertTrue(request.getCanPublish());
        assertTrue(request.getCanSubscribe());
        assertEquals(3600, request.getTokenExpirySeconds());
    }

    @Test
    void testBuilderWithCustomValues() {
        // given & when
        TokenRequest request = TokenRequest.builder()
                .roomName("test-room")
                .identity("user123")
                .canPublish(true)
                .canSubscribe(false)
                .tokenExpirySeconds(1800)
                .build();

        // then
        assertFalse(request.getCanSubscribe());
        assertEquals(1800, request.getTokenExpirySeconds());
    }

    @Test
    void testBuilderWithNullValues() {
        // given & when
        TokenRequest request = TokenRequest.builder()
                .roomName("test-room")
                .identity("user123")
                .canPublish(true)
                .canSubscribe(null)
                .tokenExpirySeconds(null)
                .build();

        // then
        assertTrue(request.getCanSubscribe());
        assertEquals(3600, request.getTokenExpirySeconds());
    }

    @Test
    void testAllArgsConstructor() {
        // given & when
        TokenRequest request = new TokenRequest(
                "test-room",
                "user123",
                true,
                null,
                null
        );

        // then
        assertEquals("test-room", request.getRoomName());
        assertEquals("user123", request.getIdentity());
        assertTrue(request.getCanPublish());
        assertTrue(request.getCanSubscribe());
        assertEquals(3600, request.getTokenExpirySeconds());
    }

    @Test
    void testDefaultValueConsistency() {
        // given
        TokenRequest builderRequest = TokenRequest.builder()
                .roomName("test")
                .identity("user")
                .canPublish(true)
                .build();

        TokenRequest constructorRequest = new TokenRequest(
                "test", "user", true, null, null
        );

        // then
        assertEquals(builderRequest.getCanSubscribe(), constructorRequest.getCanSubscribe());
        assertEquals(builderRequest.getTokenExpirySeconds(), constructorRequest.getTokenExpirySeconds());
    }
}
