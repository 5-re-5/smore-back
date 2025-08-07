package org.oreo.smore.domain.user;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.studytime.StudyTimeRepository;
import org.oreo.smore.domain.user.dto.request.UserUpdateRequest;
import org.oreo.smore.domain.user.dto.response.UserInfoResponse;
import org.oreo.smore.domain.user.dto.response.UserUpdateResponse;
import org.oreo.smore.global.common.CloudStorageManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final StudyTimeRepository studyTimeRepository;
    private final CloudStorageManager cloudStorageManager;

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

    @Transactional
    public UserUpdateResponse updateUser(Long userId, UserUpdateRequest req) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 닉네임 변경
        if (req.getNickname() != null && !req.getNickname().isBlank()) {
            if (repository.existsByNickname(req.getNickname())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다.");
            }
            user.setNickname(req.getNickname());
        }

        // 이미지 삭제
        if (Boolean.TRUE.equals(req.getRemoveImage())) {
            cloudStorageManager.deleteProfileImage(userId);
            user.setProfileUrl(null);
        }

        // 이미지 업로드
        if (req.getProfileImage() != null && !req.getProfileImage().isEmpty()) {
            cloudStorageManager.deleteProfileImage(userId);
            try {
                String uploadedUrl = cloudStorageManager.uploadProfileImage(req.getProfileImage(), userId);
                user.setProfileUrl(uploadedUrl);
            } catch (Exception e) {
                throw new RuntimeException("프로필 이미지 업로드 실패", e);
            }
        }

        // 디데이 제목
        if (req.getTargetDateTitle() != null) {
            user.setTargetDateTitle(req.getTargetDateTitle());
        }

        // 목표 날짜 (YYYY-MM-DD → LocalDateTime 00:00:00)
        if (req.getTargetDate() != null) {
            try {
                LocalDate date = LocalDate.parse(req.getTargetDate());
                user.setTargetDate(date.atStartOfDay());
            } catch (DateTimeParseException e) {
                throw new RuntimeException("목표 날짜 형식이 잘못되었습니다. (YYYY-MM-DD)", e);
            }
        }

        // 목표 공부 시간
        if (req.getGoalStudyTime() != null) {
            user.setGoalStudyTime(req.getGoalStudyTime());
        }

        // 각오
        if (req.getDetermination() != null) {
            user.setDetermination(req.getDetermination());
        }

        User saved = repository.save(user);

        return UserUpdateResponse.builder()
                .data(UserUpdateResponse.DataResponse.builder()
                        .userId(saved.getUserId())
                        .name(saved.getName())
                        .email(saved.getEmail())
                        .nickname(saved.getNickname())
                        .profileUrl(saved.getProfileUrl())
                        .createdAt(saved.getCreatedAt().toLocalDate().toString())
                        .goalStudyTime(saved.getGoalStudyTime())
                        .level(saved.getLevel())
                        .targetDateTitle(saved.getTargetDateTitle())
                        .targetDate(saved.getTargetDate() != null ? saved.getTargetDate().toLocalDate().toString() : null)
                        .determination(saved.getDetermination())
                        .build())
                .build();
    }


    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다."));

        // 오늘 공부 시간 계산
        int todayStudyMinutes = studyTimeRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .filter(study -> study.getCreatedAt().toLocalDate().isEqual(LocalDate.now()))
                .map(study -> (int) java.time.Duration.between(
                        study.getCreatedAt(), study.getDeletedAt()
                ).toMinutes())
                .orElse(0);

        return UserInfoResponse.builder()
                .data(UserInfoResponse.DataResponse.builder()
                        .userId(user.getUserId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .nickname(user.getNickname())
                        .profileUrl(user.getProfileUrl())
                        .createdAt(user.getCreatedAt().toLocalDate().toString())
                        .goalStudyTime(user.getGoalStudyTime())
                        .level(user.getLevel())
                        .targetDateTitle(user.getTargetDateTitle())
                        .targetDate(user.getTargetDate() != null ? user.getTargetDate().toLocalDate().toString() : null)
                        .determination(user.getDetermination())
                        .todayStudyMinute(todayStudyMinutes)
                        .build())
                .build();
    }

}
