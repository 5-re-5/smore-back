package org.oreo.smore.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    @Transactional
    public User registerOrUpdate(String email, String name) {
        return repository.findByEmail(email)
                .orElseGet(() -> registeUser(email, name));
    }

    private User registeUser(String email, String name) {
        User u = User.builder()
                .name(name)
                .email(email)
                .nickname(name + "@" + email)
                .createdAt(LocalDateTime.now())
                .goalStudyTime(0)
                .level("O")
                .build();
        return repository.save(u);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

}
