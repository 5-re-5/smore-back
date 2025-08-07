package org.oreo.smore.domain.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserIdentityService {

    private final UserRepository userRepository;

    private static final String DEFAULT_NICKNAME = "이름없음";


    public String generateIdentityForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        String nickname = user.getNickname();

        // nickname이 null이거나 빈 문자열인 경우 "이름없음" 사용
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = DEFAULT_NICKNAME;
            log.debug("사용자 ID {}의 nickname이 없어서 기본값 사용: [{}]", userId, DEFAULT_NICKNAME);
        }

        log.debug("사용자 ID {}의 LiveKit identity: [{}]", userId, nickname);

        return nickname;
    }

    public String getUserDisplayName(Long userId) {
        return generateIdentityForUser(userId); // 동일한 로직
    }
}
