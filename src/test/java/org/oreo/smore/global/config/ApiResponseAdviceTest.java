package org.oreo.smore.global.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.global.common.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiResponseAdviceTest {

    @InjectMocks
    private ApiResponseAdvice apiResponseAdvice;

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Test
    void ApiResponse로_이미_감싸진_경우_supports_false_반환() {
        // Given
        when(methodParameter.getParameterType()).thenReturn((Class) ApiResponse.class);

        // When
        boolean supports = apiResponseAdvice.supports(methodParameter, null);

        // Then
        assertThat(supports).isFalse();
    }

    @Test
    void 일반_객체인_경우_supports_true_반환() {
        // Given
        when(methodParameter.getParameterType()).thenReturn((Class) String.class);

        // When
        boolean supports = apiResponseAdvice.supports(methodParameter, null);

        // Then
        assertThat(supports).isTrue();
    }

    @Test
    void 일반_객체를_ApiResponse로_감싸서_반환() {
        // Given
        String testData = "test response";
        when(request.getURI()).thenReturn(URI.create("/v1/test"));

        // When
        Object result = apiResponseAdvice.beforeBodyWrite(
                testData, methodParameter, MediaType.APPLICATION_JSON,
                null, request, response);

        // Then
        assertThat(result).isInstanceOf(ApiResponse.class);
        ApiResponse<?> apiResponse = (ApiResponse<?>) result;
        assertThat(apiResponse.getData()).isEqualTo(testData);
    }

    @Test
    void String_응답은_그대로_반환() {
        // Given
        String stringResponse = "plain string";
        when(request.getURI()).thenReturn(URI.create("/v1/test"));

        // When
        Object result = apiResponseAdvice.beforeBodyWrite(
                stringResponse, methodParameter, MediaType.TEXT_PLAIN,
                null, request, response);

        // Then
        assertThat(result).isEqualTo(stringResponse);
        assertThat(result).isNotInstanceOf(ApiResponse.class);
    }

    @Test
    void actuator_경로는_그대로_반환() {
        // Given
        String actuatorResponse = "actuator data";
        when(request.getURI()).thenReturn(URI.create("/actuator/health"));

        // When
        Object result = apiResponseAdvice.beforeBodyWrite(
                actuatorResponse, methodParameter, MediaType.APPLICATION_JSON,
                null, request, response);

        // Then
        assertThat(result).isEqualTo(actuatorResponse);
        assertThat(result).isNotInstanceOf(ApiResponse.class);
    }

    @Test
    void null_데이터도_ApiResponse로_감싸서_반환() {
        // Given
        when(request.getURI()).thenReturn(URI.create("/v1/test"));

        // When
        Object result = apiResponseAdvice.beforeBodyWrite(
                null, methodParameter, MediaType.APPLICATION_JSON,
                null, request, response);

        // Then
        assertThat(result).isInstanceOf(ApiResponse.class);
        ApiResponse<?> apiResponse = (ApiResponse<?>) result;
        assertThat(apiResponse.getData()).isNull();
    }
}