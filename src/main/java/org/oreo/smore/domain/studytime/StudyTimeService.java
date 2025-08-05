package org.oreo.smore.domain.studytime;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StudyTimeService {

    private final StudyTimeRepository studyTimeRepository;

    /**
     * 공부 시작
     */
    public void startStudyTime(Long userId) {
        StudyTime studyTime = StudyTime.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now()) // 서버에서 현재 시간 저장
                .deletedAt(LocalDateTime.now()) // 서버에서 현재 시간 저장
                .build();

        studyTimeRepository.save(studyTime);
    }

    public void updateStudyTime(Long userId) {
        StudyTime latestStudyTime = studyTimeRepository.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 공부 기록이 없습니다."));

        latestStudyTime.setDeletedAt(LocalDateTime.now());
        studyTimeRepository.save(latestStudyTime);
    }
}
