package org.oreo.smore.domain.studyroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.oreo.smore.domain.participant.ParticipantRepository;
import org.oreo.smore.domain.studyroom.dto.StudyRoomDto;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.oreo.smore.global.common.CursorPage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class StudyRoomServiceTest {

    @Mock
    private StudyRoomRepository roomRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StudyRoomService studyRoomService;

    @Test
    @DisplayName("스터디룸이 없으면 빈 페이지 반환")
    void listStudyRooms_NoRooms_ReturnsEmptyPage() {
        // given
        int limit = 10;
        Pageable pageable = PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        // repository에서 빈 Slice 반환
        given(roomRepository.findAll(any(Specification.class), eq(pageable)))
                .willReturn(new SliceImpl<>(Collections.emptyList(), pageable, false));

        // when
        CursorPage<StudyRoomDto> result =
                studyRoomService.listStudyRooms(null, limit, null, null, null, false);

        // then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty(), "content가 비어 있어야 합니다");
        assertFalse(result.isHasNext(), "hasNext는 false여야 합니다");
        assertNull(result.getNextCursor(), "nextCursor는 null이어야 합니다");
    }

    @Test
    @DisplayName("단일 스터디룸 매핑 및 페이징 처리 검증")
    void listStudyRooms_SingleRoom_MappedCorrectly() {
        // given
        int limit = 1;
        StudyRoom room = mock(StudyRoom.class);

        given(room.getRoomId()).willReturn(100L);
        given(room.getUserId()).willReturn(42L);
        given(room.getMaxParticipants()).willReturn(5);

        // createdAt, title 등 기존 모킹…
        LocalDateTime now = LocalDateTime.of(2025, 8, 7, 10, 0);
        given(room.getCreatedAt()).willReturn(now);
        given(room.getTitle()).willReturn("테스트 스터디룸");

        // **category 모킹 추가**
        given(room.getCategory()).willReturn(StudyRoomCategory.EMPLOYMENT);

        Pageable pageable = PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        given(roomRepository.findAll(any(Specification.class), eq(pageable)))
                .willReturn(new SliceImpl<>(List.of(room), pageable, false));

        given(participantRepository.countByRoomIdAndLeftAtIsNull(100L)).willReturn(3L);
        User creator = new User();
        creator.setNickname("tester");
        given(userRepository.findById(42L)).willReturn(Optional.of(creator));

        // when
        CursorPage<StudyRoomDto> result =
                studyRoomService.listStudyRooms(null, limit, "검색어", "EMPLOYMENT", "latest", true);

        // then: DTO 필드들도 category 기반 로직에 맞춰 검증 가능
        assertEquals(1, result.getContent().size());
        StudyRoomDto dto = result.getContent().get(0);
        assertEquals(100L, dto.getRoomId());
        assertEquals("tester", dto.getCreator().getNickname());
        assertEquals(3, dto.getCurrentParticipants());
        assertEquals("EMPLOYMENT", dto.getCategory());  // enum.name() 값
        assertFalse(result.isHasNext());
        assertEquals(100L, result.getNextCursor());
    }
}
