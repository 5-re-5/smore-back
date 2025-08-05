package org.oreo.smore.domain.video.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.oreo.smore.domain.studyroom.StudyRoomCategory;
import org.oreo.smore.domain.studyroom.StudyRoomRepository;
import org.oreo.smore.domain.video.dto.JoinRoomRequest;
import org.oreo.smore.domain.video.exception.StudyRoomNotFoundException;
import org.oreo.smore.domain.video.exception.WrongPasswordException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyRoomValidatorTest {

    @Mock
    private StudyRoomRepository studyRoomRepository;

    @InjectMocks
    private StudyRoomValidator studyRoomValidator;

    private StudyRoom 공개방;
    private StudyRoom 비밀방;
    private JoinRoomRequest 기본요청;

    @BeforeEach
    void setUp() {
        // 공개방 설정 (비밀번호 없음)
        공개방 = StudyRoom.builder()
                .userId(1L)
                .title("공개 스터디방")
                .category(StudyRoomCategory.SELF_STUDY)
                .maxParticipants(6)
                .password(null)  // 비밀번호 없음
                .build();

        // 비밀방 설정
        비밀방 = StudyRoom.builder()
                .userId(2L)
                .title("비밀 스터디방")
                .category(StudyRoomCategory.CERTIFICATION)
                .maxParticipants(4)
                .password("1234")  // 비밀번호 있음
                .build();

        // 기본 입장 요청
        기본요청 = JoinRoomRequest.builder()
                .identity("테스트사용자")
                .canPublish(true)
                .canSubscribe(true)
                .build();
    }

    @Test
    void 공개방_입장_성공() {
        // given
        Long roomId = 1L;
        when(studyRoomRepository.findByRoomIdAndDeletedAtIsNull(roomId))
                .thenReturn(Optional.of(공개방));

        // when
        StudyRoom result = studyRoomValidator.validateRoomAccess(roomId, 기본요청);

        // then
        assertNotNull(result);
        assertEquals("공개 스터디방", result.getTitle());
        assertEquals(6, result.getMaxParticipants());
        assertNull(result.getPassword());
    }

    @Test
    void 비밀방_올바른_비밀번호_입장_성공() {
        // given
        Long roomId = 2L;
        JoinRoomRequest 비밀번호요청 = JoinRoomRequest.builder()
                .identity("테스트사용자")
                .password("1234")  // 올바른 비밀번호
                .canPublish(true)
                .canSubscribe(true)
                .build();

        when(studyRoomRepository.findByRoomIdAndDeletedAtIsNull(roomId))
                .thenReturn(Optional.of(비밀방));

        // when
        StudyRoom result = studyRoomValidator.validateRoomAccess(roomId, 비밀번호요청);

        // then
        assertNotNull(result);
        assertEquals("비밀 스터디방", result.getTitle());
        assertEquals("1234", result.getPassword());
    }

    @Test
    void 존재하지_않는_방_예외발생() {
        // given
        Long 없는방ID = 999L;
        when(studyRoomRepository.findByRoomIdAndDeletedAtIsNull(없는방ID))
                .thenReturn(Optional.empty());

        // when & then
        StudyRoomNotFoundException exception = assertThrows(StudyRoomNotFoundException.class, () -> {
            studyRoomValidator.validateRoomAccess(없는방ID, 기본요청);
        });

        assertTrue(exception.getMessage().contains("방을 찾을 수 없습니다"));
        assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    void 비밀방_비밀번호_없이_입장_예외발생() {
        // given
        Long roomId = 2L;
        when(studyRoomRepository.findByRoomIdAndDeletedAtIsNull(roomId))
                .thenReturn(Optional.of(비밀방));

        // when & then
        WrongPasswordException exception = assertThrows(WrongPasswordException.class, () -> {
            studyRoomValidator.validateRoomAccess(roomId, 기본요청);  // 비밀번호 없는 요청
        });

        assertEquals("이 방은 비밀번호가 필요합니다.", exception.getMessage());
    }

    @Test
    void 비밀방_틀린_비밀번호_예외발생() {
        // given
        Long roomId = 2L;
        JoinRoomRequest 틀린비밀번호요청 = JoinRoomRequest.builder()
                .identity("테스트사용자")
                .password("5678")  // 틀린 비밀번호
                .canPublish(true)
                .canSubscribe(true)
                .build();

        when(studyRoomRepository.findByRoomIdAndDeletedAtIsNull(roomId))
                .thenReturn(Optional.of(비밀방));

        // when & then
        WrongPasswordException exception = assertThrows(WrongPasswordException.class, () -> {
            studyRoomValidator.validateRoomAccess(roomId, 틀린비밀번호요청);
        });

        assertEquals("비밀번호가 틀렸습니다.", exception.getMessage());
    }

    @Test
    void 공백_비밀번호_처리() {
        // given
        Long roomId = 2L;
        JoinRoomRequest 공백비밀번호요청 = JoinRoomRequest.builder()
                .identity("테스트사용자")
                .password("   ")  // 공백 비밀번호
                .canPublish(true)
                .canSubscribe(true)
                .build();

        when(studyRoomRepository.findByRoomIdAndDeletedAtIsNull(roomId))
                .thenReturn(Optional.of(비밀방));

        // when & then
        WrongPasswordException exception = assertThrows(WrongPasswordException.class, () -> {
            studyRoomValidator.validateRoomAccess(roomId, 공백비밀번호요청);
        });

        assertEquals("이 방은 비밀번호가 필요합니다.", exception.getMessage());
    }

    @Test
    void 방장_여부_확인_성공() {
        // given
        Long 방장ID = 1L;
        Long 일반사용자ID = 2L;

        // when & then
        assertTrue(studyRoomValidator.isRoomOwner(공개방, 방장ID));
        assertFalse(studyRoomValidator.isRoomOwner(공개방, 일반사용자ID));
    }

    @Test
    void 최대인원_검증_통과() {
        // given
        Long roomId = 1L;
        when(studyRoomRepository.findByRoomIdAndDeletedAtIsNull(roomId))
                .thenReturn(Optional.of(공개방));

        // when & then
        assertDoesNotThrow(() -> {
            studyRoomValidator.validateRoomAccess(roomId, 기본요청);
        });
    }

    @Test
    void 방_정보_로깅_테스트() {
        // when & then - 예외 없이 실행되는지 확인
        assertDoesNotThrow(() -> {
            studyRoomValidator.logRoomInfo(공개방);
            studyRoomValidator.logRoomInfo(비밀방);
        });
    }
}
