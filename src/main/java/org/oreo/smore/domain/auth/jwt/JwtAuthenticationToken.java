package org.oreo.smore.domain.auth.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final String userId;

    public JwtAuthenticationToken(String userId, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.userId = userId;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";  // JWT 에서는 credential 정보가 따로 없으므로 빈 문자열 리턴
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }
}
