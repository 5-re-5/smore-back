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
    public StudyTime startStudy(Long userId) {
        StudyTime studyTime = StudyTime.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now()) // 서버에서 현재 시간 저장
                .deletedAt(LocalDateTime.now()) // 서버에서 현재 시간 저장
                .build();

        return studyTimeRepository.save(studyTime);
    }

    public void updateStudy(Long userId) {
    }
}
