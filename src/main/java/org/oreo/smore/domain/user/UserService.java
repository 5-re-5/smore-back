package org.oreo.smore.domain.user;

import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.point.Point;
import org.oreo.smore.domain.point.PointRepository;
import org.oreo.smore.domain.studytime.StudyTime;
import org.oreo.smore.domain.studytime.StudyTimeRepository;
import org.oreo.smore.domain.user.dto.request.UserUpdateRequest;
import org.oreo.smore.domain.user.dto.response.UserInfoResponse;
import org.oreo.smore.domain.user.dto.response.UserUpdateResponse;
import org.oreo.smore.global.common.CloudStorageManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final StudyTimeRepository studyTimeRepository;
    private final CloudStorageManager cloudStorageManager;
    private final PointRepository pointRepository;

    @Transactional
    public User registerOrUpdate(String email, String name) {
        return repository.findByEmail(email)
                .orElseGet(() -> registeUser(email, name));
    }

    private User registeUser(String email, String name) {
        User u = User.builder()
                .name(name)
                .email(email)
                .nickname(UUID.randomUUID().toString())
                .profileUrl("https://oreost.blob.core.windows.net/oreoct/default/user.webp?t=202508130152")
                .createdAt(LocalDateTime.now())
                .goalStudyTime(60)
                .level("O")
                .targetDateTitle("스모어 시작")
                .targetDate(LocalDateTime.now())
                .determination("파이팅")
                .build();

        User savedUser = repository.save(u);
        savedUser.setNickname("OREO" + savedUser.getUserId());

        Point point = Point.builder()
                .userId(savedUser.getUserId())
                .delta(550)
                .reason("가입 기념 지급")
                .timestamp(LocalDateTime.now())
                .build();
        pointRepository.save(point);

        return repository.save(savedUser);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @CacheEvict(value = "user:profile", key = "#userId")
    @Transactional
    public UserUpdateResponse updateUser(Long userId, UserUpdateRequest req) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 닉네임 변경
        if (req.getNickname() != null && !req.getNickname().isBlank()) {
            if (repository.existsByNickname(req.getNickname())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다.");
            }
            if (req.getNickname().toUpperCase().startsWith("OREO")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OREO로 시작하는 닉네임은 사용할 수 없습니다.");
            }
            user.setNickname(req.getNickname());
        }

        // 이미지 삭제
        if (Boolean.TRUE.equals(req.getRemoveImage())) {
            cloudStorageManager.deleteProfileImage(userId);
            user.setProfileUrl("https://oreost.blob.core.windows.net/oreos/default/user.png?t=202508130152");
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
                .build();
    }


    @Cacheable(value = "user:profile", key = "#userId")
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다."));

        // 오늘 공부 시간 계산
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<StudyTime> allRecords = studyTimeRepository.findAllByUserIdAndCreatedAtBetween(
                userId,
                startOfDay.minusDays(1),  // 하루 전까지도 시작할 수 있으므로
                endOfDay.plusDays(1)      // 자정 넘어가는 케이스도 포함
        );

        int todayStudySeconds = 0;

        for (StudyTime record : allRecords) {
            LocalDateTime start = record.getCreatedAt();
            LocalDateTime end = record.getDeletedAt();

            if (end == null) {
                // 아직 진행 중인 공부 세션은 현재 시간까지로 계산
                end = LocalDateTime.now();
            }

            // 오늘 날짜 범위와 겹치는 구간만 계산
            LocalDateTime effectiveStart = start.isBefore(startOfDay) ? startOfDay : start;
            LocalDateTime effectiveEnd = end.isAfter(endOfDay) ? endOfDay : end;

            if (!effectiveStart.isAfter(effectiveEnd)) {
                todayStudySeconds += Duration.between(effectiveStart, effectiveEnd).getSeconds();
            }
        }

        int todayStudyMinutes = todayStudySeconds / 60;


        return UserInfoResponse.builder()
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
                .build();
    }

}
