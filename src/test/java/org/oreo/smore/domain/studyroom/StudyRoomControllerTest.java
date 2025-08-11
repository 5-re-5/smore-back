
package org.oreo.smore.domain.studyroom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.oreo.smore.domain.studyroom.dto.StudyRoomInfoReadResponse;
import org.oreo.smore.global.common.CursorPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(StudyRoomController.class)
class StudyRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyRoomService studyRoomService;

    @MockitoBean
    private StudyRoomCreationService studyRoomCreationService;

    @Test
    @DisplayName("GET /v1/study-rooms 조회 시 빈 페이지 반환")
    void listStudyRooms_ShouldReturnEmptyPage() throws Exception {
        // given: Service가 빈 페이지를 반환하도록 Mock 설정
        CursorPage<StudyRoomInfoReadResponse> emptyPage = new CursorPage<>();
        emptyPage.setCursorId(1L);
        emptyPage.setSize(20);
        emptyPage.setContent(Collections.emptyList());
        emptyPage.setHasNext(false);
        emptyPage.setNextCursor(null);

        given(studyRoomService.listStudyRooms(
                anyLong(), anyInt(), any(), any(), anyString(), anyBoolean()
        )).willReturn(emptyPage);

        // when & then: 기본 파라미터로 조회 요청 시
        mockMvc.perform(get("/v1/study-rooms")
                        .param("page", "1")
                        .param("limit", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // 🔥 $.data.* 경로로 수정 (ApiResponse 래핑 때문에)
                .andExpect(jsonPath("$.data.cursorId").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                // content 배열이 비어있는지 검증
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    @DisplayName("GET /v1/study-rooms 조회 시 기본 정렬(latest) 검증")
    void listStudyRooms_WithSortParam() throws Exception {
        // given: 하나의 더미 DTO 반환
        StudyRoomInfoReadResponse dto = StudyRoomInfoReadResponse.builder()
                .roomId(100L)
                .title("테스트 스터디룸")
                .build();

        CursorPage<StudyRoomInfoReadResponse> page = new CursorPage<>();
        page.setCursorId(100L);
        page.setSize(1);
        page.setContent(Collections.singletonList(dto));
        page.setHasNext(false);
        page.setNextCursor(null);

        given(studyRoomService.listStudyRooms(
                eq(5L), eq(1), isNull(), isNull(), eq("latest"), eq(true)
        )).willReturn(page);

        // when & then: 추가 파라미터를 포함하여 조회 요청
        mockMvc.perform(get("/v1/study-rooms")
                        .param("page", "5")
                        .param("limit", "1")
                        .param("hide-full-rooms", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // 🔥 $.data.* 경로로 수정
                .andExpect(jsonPath("$.data.cursorId").value(100))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.content[0].roomId").value(100))
                .andExpect(jsonPath("$.data.content[0].title").value("테스트 스터디룸"));
    }

    @Test
    @DisplayName("GET /v1/study-rooms 여러 파라미터로 조회 시 서비스 호출 및 응답 검증")
    void listStudyRooms_WithAllParams() throws Exception {
        // given
        StudyRoomInfoReadResponse dto = StudyRoomInfoReadResponse.builder()
                .roomId(200L)
                .title("여러 파람 테스트")
                .build();
        CursorPage<StudyRoomInfoReadResponse> page = new CursorPage<>();
        page.setCursorId(200L);
        page.setSize(1);
        page.setContent(Collections.singletonList(dto));
        page.setHasNext(false);
        page.setNextCursor(null);

        // service가 정확한 파라미터로 호출되도록 목 설정
        given(studyRoomService.listStudyRooms(
                eq(2L),           // page=2
                eq(3),            // limit=3
                eq("hello"),      // search=hello
                eq("LANGUAGE"),   // category=LANGUAGE
                eq("popular"),    // sort=popular
                eq(false)         // hide_full_rooms=false
        )).willReturn(page);

        // when & then
        mockMvc.perform(get("/v1/study-rooms")
                        .param("page", "2")
                        .param("limit", "3")
                        .param("search", "hello")
                        .param("category", "LANGUAGE")
                        .param("sort", "popular")
                        .param("hide-full-rooms", "false")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // 🔥 $.data.* 경로로 수정
                .andExpect(jsonPath("$.data.cursorId").value(200))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.content[0].roomId").value(200))
                .andExpect(jsonPath("$.data.content[0].title").value("여러 파람 테스트"));
    }
}