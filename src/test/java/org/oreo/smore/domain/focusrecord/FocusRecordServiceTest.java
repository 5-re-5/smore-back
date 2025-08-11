//package org.oreo.smore.domain.focusrecord;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.*;
//import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//import java.util.*;
//import java.util.stream.IntStream;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//import static org.junit.jupiter.api.Assertions.*;
//
//class FocusRecordServiceTest {
//
//    @Mock
//    private FocusRecordRepository repository;
//
//    @Mock
//    private FocusFeedbackService feedbackService;    // 추가: 목 빈 선언
//
//    @InjectMocks
//    private FocusRecordService service;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    void getFocusRecords_withValidRecords_returnsResponse() {
//        // given
//        Long userId = 1L;
//        String tzOffset = "+09:00";
//        Instant now = Instant.now();
//
//        FocusRecord r1 = FocusRecord.builder()
//                .recordId(1L)
//                .userId(userId)
//                .timestamp(now.minus(10, ChronoUnit.DAYS))
//                .status(100)
//                .build();
//        FocusRecord r2 = FocusRecord.builder()
//                .recordId(2L)
//                .userId(userId)
//                .timestamp(now.minus(5, ChronoUnit.DAYS))
//                .status(50)
//                .build();
//
//        when(repository.findByUserIdAndTimestampAfter(eq(userId), any(Instant.class)))
//                .thenReturn(Arrays.asList(r1, r2));
//
//        // 피드백 서비스는 항상 "테스트 피드백" 반환하도록 stub
//        when(feedbackService.generateOneLineFeedback(
//                any(), any(), anyInt(), any()))
//                .thenReturn("테스트 피드백");
//
//        // when
//        FocusRecordsResponse resp = service.getFocusRecords(userId, tzOffset);
//
//        // then
//        assertNotNull(resp);
//        var insights = resp.getData().getAiInsights();
//        assertNotNull(insights);
//
//        // 1) feedback 은 stub 값으로
//        assertEquals("테스트 피드백", insights.getFeedback());
//
//        // 2) averageFocusDuration 은 “시간 단위 반올림” 로직에 따라 0
//        assertEquals(0, insights.getAverageFocusDuration());
//
//        // 3) focusTrack.labels 는 "00"부터 "23" 총 24개
//        var track = insights.getFocusTrack();
//        assertEquals(24, track.getLabels().size());
//        assertEquals("00", track.getLabels().get(0));
//        assertEquals("23", track.getLabels().get(23));
//
//        // 4) focusTrack.scores 에 non-zero 값이 하나만 있고, 값은 75
//        assertEquals(24, track.getScores().size());
//        long nonZeroCount = track.getScores().stream().filter(s -> s > 0).count();
//        assertEquals(1, nonZeroCount);
//        int idx = IntStream.range(0, track.getScores().size())
//                .filter(i -> track.getScores().get(i) > 0)
//                .findFirst()
//                .orElseThrow();
//        assertEquals(75, track.getScores().get(idx));
//
//        // 5) best/worst focus time DTO 기본값
//        var best = insights.getBestFocusTime();
//        var worst = insights.getWorstFocusTime();
//        assertEquals("00:00", best.getStart());
//        assertEquals("02:00", best.getEnd());
//        assertEquals("00:00", worst.getStart());
//        assertEquals("02:00", worst.getEnd());
//    }
//
//    @Test
//    void getFocusRecords_withNoRecords_throwsNotFound() {
//        Long userId = 1L;
//        when(repository.findByUserIdAndTimestampAfter(eq(userId), any(Instant.class)))
//                .thenReturn(Collections.emptyList());
//
//        ResponseStatusException ex = assertThrows(
//                ResponseStatusException.class,
//                () -> service.getFocusRecords(userId, "+09:00")
//        );
//        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
//    }
//}
