package org.oreo.smore.domain.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.auth.user.AuthUser;
import org.oreo.smore.domain.auth.user.AuthUserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthUserService authUserService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        // 1) 기존 DefaultOAuth2UserService 로부터 OAuth2User 정보 조회
        OAuth2User original = super.loadUser(userRequest);

        // 2) 이메일 추출 (구글 기준)
        String email = original.getAttribute("email");
        String name = original.getAttribute("name");

        // 3) 인증용 User 엔티티 저장 or 업데이트
        AuthUser user = authUserService.registerOrUpdate(email, name);

        // 4) 속성 복사 + userId 추가
        Map<String, Object> props = new HashMap<>(original.getAttributes());
        props.put("userId", user.getUserId());

        // 5) DefaultOAuth2User 로 재생성
        //    - 권한은 ROLE_USER 로 고정 (필요에 따라 변경)
        //    - nameAttributeKey 를 "userId" 로 설정하면, authentication.getName() 으로 userId 를 꺼낼 수 있습니다.
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                props,
                "userId"
        );
    }
}
