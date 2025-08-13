package org.oreo.smore.global.config;

import org.oreo.smore.global.common.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@ControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {

        Class<?> type = returnType.getParameterType();

        // 이미 ApiResponse면 제외
        if (ApiResponse.class.isAssignableFrom(type)) return false;

        // String(CharSequence) 반환은 제외 (String 컨버터와 충돌 방지)
        if (CharSequence.class.isAssignableFrom(type)) return false;

        // 파일/리소스/스트리밍/바이너리는 제외
        if (Resource.class.isAssignableFrom(type)) return false;
        if (StreamingResponseBody.class.isAssignableFrom(type)) return false;
        if (type.equals(byte[].class)) return false;

        // 선택된 컨버터가 String이면 제외 (ResponseEntity<String> 등)
        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) return false;

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // 특정 경로는 제외
        String path = request.getURI().getPath();
        if (path.contains("/actuator")
                || path.contains("/h2-console")
                || path.contains("/error")) {
            return body;
        }

        // 이미 ApiResponse면 그대로 통과
        if (body instanceof ApiResponse) {
            return body;
        }

        // String 컨버터가 선택된 경우 래핑 금지 (핵심)
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            return body;
        }

        // JSON이 아닌 경우(파일 다운로드 등) 손대지 않음
        if (selectedContentType == null
                || !selectedContentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            return body;
        }

        // body가 null이면 통일된 포맷으로 래핑
        if (body == null) {
            return ApiResponse.of(null);
        }

        // 그 외 객체는 ApiResponse로 래핑
        return ApiResponse.of(body);
    }
}