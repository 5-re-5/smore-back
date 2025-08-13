package org.oreo.smore.domain.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.oreo.smore.domain.studyroom.StudyRoom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    // StudyRoom 생성 시 ChatRoom 자동 생성
    @Transactional
    public ChatRoom createChatRoom(StudyRoom studyRoom) {
        log.info("🏠 채팅방 생성 시작 - StudyRoom ID: {}, 제목: {}",
                studyRoom.getRoomId(), studyRoom.getTitle());

        try {
            // 이미 존재하는지 확인
            Optional<ChatRoom> existingRoom = chatRoomRepository.findByStudyRoomId(studyRoom.getRoomId());
            if (existingRoom.isPresent()) {
                log.warn("⚠️ 이미 존재하는 채팅방 - StudyRoom ID: {}", studyRoom.getRoomId());
                return existingRoom.get();
            }

            // 새 채팅방 생성
            ChatRoom chatRoom = ChatRoom.builder()
                    .studyRoom(studyRoom)
                    .build();

            ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

            log.info("✅ 채팅방 생성 완료 - StudyRoom ID: {}, ChatRoom ID: {}",
                    studyRoom.getRoomId(), savedChatRoom.getStudyRoomId());

            return savedChatRoom;

        } catch (Exception e) {
            log.error("❌ 채팅방 생성 실패 - StudyRoom ID: {}, 오류: {}",
                    studyRoom.getRoomId(), e.getMessage(), e);
            throw new RuntimeException("채팅방 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteChatRoomByStudyRoom(Long studyRoomId) {
        log.info("🗑️ 채팅방 삭제 시작 - StudyRoom ID: {}", studyRoomId);

        try {
            // ChatRoom 조회
            Optional<ChatRoom> chatRoomOpt = chatRoomRepository.findByStudyRoomId(studyRoomId);

            if (chatRoomOpt.isEmpty()) {
                log.warn("⚠️ 삭제할 채팅방이 없음 - StudyRoom ID: {}", studyRoomId);
                return;
            }

            ChatRoom chatRoom = chatRoomOpt.get();

            // 1. 관련 채팅 메시지 소프트 삭제
            chatMessageRepository.softDeleteAllMessagesByRoomId(studyRoomId);
            log.info("✅ 채팅 메시지 소프트 삭제 완료 - StudyRoom ID: {}", studyRoomId);

            // 2. ChatRoom 비활성화
            chatRoom.deactivate();
            chatRoomRepository.save(chatRoom);
            log.info("✅ 채팅방 비활성화 완료 - StudyRoom ID: {}", studyRoomId);

        } catch (Exception e) {
            log.error("❌ 채팅방 삭제 실패 - StudyRoom ID: {}, 오류: {}",
                    studyRoomId, e.getMessage(), e);
            throw new RuntimeException("채팅방 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    public Optional<ChatRoom> getActiveChatRoom(Long studyRoomId) {
        return chatRoomRepository.findActiveByStudyRoomId(studyRoomId);
    }

    public boolean existsChatRoom(Long studyRoomId) {
        return chatRoomRepository.findByStudyRoomId(studyRoomId).isPresent();
    }


    @Transactional
    public void updateMessageCount(Long studyRoomId) {
        try {
            Long messageCount = chatMessageRepository.countActiveMessagesByRoomId(studyRoomId);
            chatRoomRepository.updateMessageCount(studyRoomId, messageCount);

            log.debug("📊 채팅방 메시지 개수 업데이트 - StudyRoom ID: {}, 메시지 수: {}",
                    studyRoomId, messageCount);

        } catch (Exception e) {
            log.error("❌ 메시지 개수 업데이트 실패 - StudyRoom ID: {}, 오류: {}",
                    studyRoomId, e.getMessage());
        }
    }
}