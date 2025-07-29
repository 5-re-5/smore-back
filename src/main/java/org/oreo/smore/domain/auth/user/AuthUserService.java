package org.oreo.smore.domain.auth.user;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthUserService {

    private final UserService userService;

    @Transactional
    public AuthUser registerOrUpdate(String email, String name) {
        User userInfo = userService.registerOrUpdate(email, name);

        // 2) AuthUser DTO 로 매핑
        return AuthUser.builder()
                .userId(userInfo.getUserId())
                .email(userInfo.getEmail())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthUser findByEmail(String email) {
        User full = userService.findByEmail(email);
        return AuthUser.builder()
                .userId(full.getUserId())
                .email(full.getEmail())
                .build();
    }
}
