package org.oreo.smore.global.config;

import org.oreo.smore.global.common.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // 이미 ApiResponse로 감싸져 있으면 제외
        return !returnType.getParameterType().equals(ApiResponse.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 특정 경로는 제외 (필요에 따라 수정)
        String path = request.getURI().getPath();
        if (path.contains("/actuator") ||
                path.contains("/h2-console") ||
                path.contains("/error")) {
            return body;
        }

        if (selectedContentType != null &&
                !selectedContentType.includes(MediaType.APPLICATION_JSON)) {
            return body;
        }

        // ResponseEntity의 body가 null인 경우 처리
        if (body == null) {
            return ApiResponse.of(null);
        }

        return ApiResponse.of(body);
    }
}