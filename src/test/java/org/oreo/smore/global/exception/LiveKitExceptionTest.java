package org.oreo.smore.global.exception;


import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class LiveKitExceptionTest {
    @Test
    void testDefaultConstructor() {
        // given & when
        LiveKitException exception = new LiveKitException();

        // then
        assertEquals("LiveKit 서버 통신 중 오류가 발생했습니다.", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testMessageConstructor() {
        // given
        String errorMessage = "토큰 생성에 실패했습니다.";

        // when
        LiveKitException exception = new LiveKitException(errorMessage);

        // then
        assertEquals(errorMessage, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testMessageAndCauseConstructor() {
        // given
        String errorMessage = "방 생성에 실패했습니다.";
        RuntimeException cause = new RuntimeException("네트워크 오류");

        // when
        LiveKitException exception = new LiveKitException(errorMessage, cause);

        // then
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testCauseOnlyConstructor() {
        // given
        RuntimeException cause = new RuntimeException("서버 연결 실패");

        // when
        LiveKitException exception = new LiveKitException(cause);

        // then
        assertEquals("LiveKit 서버 통신 중 오류가 발생했습니다.", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testResponseStatusAnnotation() {
        // given & when
        ResponseStatus responseStatus = LiveKitException.class.getAnnotation(ResponseStatus.class);

        // then
        assertNotNull(responseStatus, "@ResponseStatus 어노테이션이 없습니다.");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseStatus.value());
    }

    @Test
    void testIsRuntimeException() {
        // given & when
        LiveKitException exception = new LiveKitException("테스트");

        // then
        assertTrue(exception instanceof RuntimeException, "RuntimeException을 상속받아야 합니다.");
    }

    @Test
    void testExceptionChaining() {
        // given
        Exception originalCause = new Exception("원본 오류");
        RuntimeException intermediateCause = new RuntimeException("중간 오류", originalCause);

        // when
        LiveKitException exception = new LiveKitException("최종 오류", intermediateCause);

        // then
        assertEquals("최종 오류", exception.getMessage());
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(originalCause, exception.getCause().getCause());
    }
}
