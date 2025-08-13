package org.oreo.smore.domain.video.service;

import io.livekit.server.RoomServiceClient;
import jakarta.annotation.PreDestroy;
import livekit.LivekitModels;
import livekit.LivekitRoom;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.video.exception.LiveKitException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class LiveKitRoomService {

    private final RoomServiceClient roomServiceClient;
    private final String serverUrl;

    public LiveKitRoomService(
            @Value("${livekit.apiKey}") String apiKey,
            @Value("${livekit.apiSecret}") String apiSecret,
            @Value("${livekit.url}") String serverUrl) {
        this.serverUrl = serverUrl;

        if (apiKey == null || apiSecret == null || serverUrl == null) {
            throw new IllegalArgumentException("LiveKit 설정이 누락되었습니다. apiKey, apiSecret, serverUrl을 확인하세요.");
        }

        try {
            this.roomServiceClient = RoomServiceClient.createClient(serverUrl, apiKey, apiSecret);
            log.info("✅ LiveKit RoomServiceClient 초기화 완료 → 서버: {}", serverUrl);
        } catch (Exception e) {
            log.error("❌ LiveKit RoomServiceClient 초기화 실패 → 서버: {}, 오류: {}", serverUrl, e.getMessage());
            throw new LiveKitException("LiveKit 클라이언트 초기화에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // LiveKit 방 삭제
    public void deleteRoom(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            log.warn("방 이름이 비어있음 - 삭제 건너뛰기");
            return;
        }
        log.info("LiveKit 방 삭제 시작 → 방: [{}]", roomName);

        try {
            // Retrofit Call 패턴 사용
            Call<Void> call =  roomServiceClient.deleteRoom(roomName);
            Response<Void> response = call.execute();

            if (response.isSuccessful()) {
                log.info("✅ LiveKit 방 삭제 성공 → 방: [{}]", roomName);
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().toString() : "Unknown error";

                if (response.code() == 404 || errorBody.contains("not found") || errorBody.contains("does not exist")) {
                    log.info("LiveKit 방이 이미 존재하지 않음 → 방: [{}]", roomName);
                    return;
                }

                log.error("❌ LiveKit 방 삭제 실패 → 방: [{}], HTTP: {}, 응답: {}", roomName, response.code(), errorBody);
                throw new LiveKitException("LiveKit 방 삭제에 실패했습니다. HTTP: " + response.code() + ", " + errorBody);
            }

        } catch (IOException e) {
            log.error("❌ LiveKit 방 삭제 중 네트워크 오류 → 방: [{}], 오류: {}", roomName, e.getMessage(), e);
            throw new LiveKitException("LiveKit 방 삭제 중 네트워크 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ LiveKit 방 삭제 실패 → 방: [{}], 오류: {}", roomName, e.getMessage(), e);
            throw new LiveKitException("LiveKit 방 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    // Livekit 방 목록 조회
    public List<LivekitModels.Room> listRooms() {
        try {
            Call<List<LivekitModels.Room>> call = roomServiceClient.listRooms();
            Response<List<LivekitModels.Room>> response = call.execute();

            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                log.error("방 목록 조회 실패 - HTTP: {}, 응답: {}", response.code(), errorBody);
                return List.of(); // 빈 리스트 반환
            }

        } catch (Exception e) {
            log.warn("LiveKit 방 목록 조회 실패: {}", e.getMessage());
            return List.of(); // 빈 리스트 반환
        }
    }

    // LiveKit 방 존재 여부
    private boolean isRoomExists(String roomName) {
        try {
            List<LivekitModels.Room> rooms = listRooms();
            boolean exists = rooms.stream().anyMatch(room -> room.getName().equals(roomName));

            log.debug("LiveKit 방 존재 확인 → 방: [{}], 존재: {}", roomName, exists);
            return exists;

        } catch (Exception e) {
            log.warn("LiveKit 방 존재 확인 실패 → 방: [{}], 오류: {}", roomName, e.getMessage());
            return false;
        }
    }

    public void deleteRoomSafely(String roomName) {
        try {
            deleteRoom(roomName);
        } catch (Exception e) {
            log.error("LiveKit 방 삭제 실패 (무시됨) → 방: [{}], 오류: {}", roomName, e.getMessage());
        }
    }

    // 방 이름 생성
    public static String generateRoomName(Long roomId) {
        if (roomId == null) {
            throw new IllegalArgumentException("roomId는 null일 수 없습니다.");
        }
        return "study-room-" + roomId;
    }

    // 비동기로 방 삭제
    public void deleteRoomAsync(String roomName) {
        new Thread(() -> {
            try {
                deleteRoom(roomName);
            } catch (Exception e) {
                log.error("비동기 LiveKit 방 삭제 실패 (무시됨) → 방: [{}], 오류: {}", roomName, e.getMessage());
            }
        }, "livekit-delete-" + roomName).start();
    }

    // 연결 상태 확인
    public boolean isHealthy() {
        try {
            Call<List<LivekitModels.Room>> call = roomServiceClient.listRooms();
            Response<List<LivekitModels.Room>> response = call.execute();
            return response.isSuccessful();
        } catch (Exception e) {
            log.warn("LiveKit 연결 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("LiveKit RoomServiceClient 정리 중...");
    }
}