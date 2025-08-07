package org.oreo.smore.domain.focusrecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.oreo.smore.domain.focusrecord.dto.FocusRecordsResponse;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class FocusRecordControllerTest {

    @Mock
    private FocusRecordService service;

    @InjectMocks
    private FocusRecordController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getFocusRecords_authorized_returnsOk() {
        Long userId = 1L;
        String tz = "+09:00";
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId.toString());

        FocusRecordsResponse dummy = new FocusRecordsResponse();
        when(service.getFocusRecords(userId, tz)).thenReturn(dummy);

        ResponseEntity<Object> resp = controller.getFocusRecords(userId, tz, auth);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dummy, resp.getBody());
    }

    @Test
    void getFocusRecords_unauthorized_returnsUnauthorized() {
        Long userId = 1L;
        String tz = "+09:00";
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("2"); // 다른 사용자

        ResponseEntity<Object> resp = controller.getFocusRecords(userId, tz, auth);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertNull(resp.getBody());
    }
}
