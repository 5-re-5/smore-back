package org.oreo.smore.domain.focusrecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class FocusRecordServiceTest {

    @Mock
    private FocusRecordRepository repository;

    @InjectMocks
    private FocusRecordService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getFocusRecords_withValidRecords_returnsResponse() {
        // given
        Long userId = 1L;
        String tzOffset = "+09:00";
        Instant now = Instant.now();
        FocusRecord r1 = FocusRecord.builder()
                .recordId(1L)
                .userId(userId)
                .timestamp(now.minus(10, ChronoUnit.DAYS))
                .status(100)
                .build();
        FocusRecord r2 = FocusRecord.builder()
                .recordId(2L)
                .userId(userId)
                .timestamp(now.minus(5, ChronoUnit.DAYS))
                .status(50)
                .build();
        when(repository.findByUserIdAndTimestampAfter(eq(userId), any(Instant.class)))
                .thenReturn(Arrays.asList(r1, r2));

        // when
        FocusRecordsResponse resp = service.getFocusRecords(userId, tzOffset);

        // then
        assertNotNull(resp);
        var insights = resp.getData().getAiInsights();
        assertNotNull(insights);

        System.out.println(insights);

        // 1) feedback 은 그대로
        assertNotNull(insights.getFeedback());

        // 2) averageFocusDuration 은 “시간 단위 반올림” 로직에 따라 0
        assertEquals(0, insights.getAverageFocusDuration());

        // 3) focusTrack.labels 는 "00"부터 "23" 총 24개
        var track = insights.getFocusTrack();
        assertEquals(24, track.getLabels().size());
        assertEquals("00", track.getLabels().get(0));
        assertEquals("23", track.getLabels().get(23));

        // 4) focusTrack.scores 에는 100,50 상태값이 해당하는 “시간대”만 합산되는데
        //    r1.timestamp 기준이 UTC→+09:00 으로 변환했을 때 (예: 10일 전 오전 9시),
        //    index 9 또는 10 에 75가 들어갑니다. (예시에선 index=9 로 가정)
        assertEquals(24, track.getScores().size());
        // non-zero 요소가 하나만 존재하는지
        long nonZeroCount = track.getScores().stream().filter(s -> s > 0).count();
        assertEquals(1, nonZeroCount);
        // 그리고 그 값이 75 인지
        int idx = IntStream.range(0, track.getScores().size())
                .filter(i -> track.getScores().get(i) > 0)
                .findFirst()
                .orElseThrow();
        assertEquals(75, track.getScores().get(idx));

        // 5) best/worst focus time DTO 는 기본값(00:00~02:00 등) 그대로
        var best = insights.getBestFocusTime();
        var worst = insights.getWorstFocusTime();
        assertEquals("00:00", best.getStart());
        assertEquals("02:00", best.getEnd());
        assertEquals("00:00", worst.getStart());
        assertEquals("02:00", worst.getEnd());
    }

    @Test
    void getFocusRecords_withNoRecords_throwsNotFound() {
        Long userId = 1L;
        when(repository.findByUserIdAndTimestampAfter(eq(userId), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.getFocusRecords(userId, "+09:00")
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
