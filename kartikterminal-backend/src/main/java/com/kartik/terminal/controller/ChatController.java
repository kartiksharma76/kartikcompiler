package com.kartik.terminal.controller;

import com.kartik.terminal.entity.ChatMessage;
import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.ChatMessageRepository;
import com.kartik.terminal.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final com.kartik.terminal.service.AIService aiService;

    @MessageMapping("/chat.sendMessage")
    public void processMessage(@Payload ChatMessageRequest request) {
        log.info("Received chat message from {} to {}: {}", request.getSenderId(), request.getRecipientId(), request.getContent());
        
        User sender = userRepository.findById(request.getSenderId()).orElse(null);
        
        if (request.getRecipientId() == 0L && sender != null) {
            ChatResponse msgAck = new ChatResponse(
                System.currentTimeMillis(), sender.getId(), sender.getUsername(), 0L, request.getContent(), LocalDateTime.now().toString()
            );
            messagingTemplate.convertAndSend("/queue/chat/" + sender.getId(), msgAck);
            
            String aiReply = aiService.callNvidiaAI("User says: " + request.getContent());
            ChatResponse aiMsg = new ChatResponse(
                System.currentTimeMillis() + 1, 0L, "AI Assistant", sender.getId(), aiReply, LocalDateTime.now().toString()
            );
            messagingTemplate.convertAndSend("/queue/chat/" + sender.getId(), aiMsg);
            return;
        }

        User recipient = userRepository.findById(request.getRecipientId()).orElse(null);
        
        if (sender != null && recipient != null) {
            ChatMessage savedMsg = ChatMessage.builder()
                    .sender(sender)
                    .recipient(recipient)
                    .content(request.getContent())
                    .build();
            chatMessageRepository.save(savedMsg);

            ChatResponse response = new ChatResponse(
                savedMsg.getId(),
                sender.getId(),
                sender.getUsername(),
                recipient.getId(),
                savedMsg.getContent(),
                LocalDateTime.now().toString()
            );
            
            messagingTemplate.convertAndSend("/queue/chat/" + recipient.getId(), response);
            messagingTemplate.convertAndSend("/queue/chat/" + sender.getId(), response);
            
        } else if (sender != null) {
            ChatResponse alert = new ChatResponse(
                -1L, -1L, "System", sender.getId(), "[System]: User #" + request.getRecipientId() + " not found.", LocalDateTime.now().toString()
            );
            messagingTemplate.convertAndSend("/queue/chat/" + sender.getId(), alert);
        }
    }

    @GetMapping("/api/chat/history/{userId1}/{userId2}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getChatHistory(@PathVariable Long userId1, @PathVariable Long userId2) {
        return ResponseEntity.ok(
            chatMessageRepository.findBySenderIdAndRecipientIdOrSenderIdAndRecipientIdOrderByTimestampAsc(
                userId1, userId2, userId2, userId1
            ).stream().map(msg -> new ChatResponse(
                msg.getId(),
                msg.getSender().getId(),
                msg.getSender().getUsername(),
                msg.getRecipient().getId(),
                msg.getContent(),
                msg.getTimestamp() != null ? msg.getTimestamp().toString() : LocalDateTime.now().toString()
            )).collect(java.util.stream.Collectors.toList())
        );
    }

    @Data
    public static class ChatMessageRequest {
        private Long senderId;
        private Long recipientId;
        private String content;
    }

    @Data
    public static class ChatResponse {
        private final Long id;
        private final Long senderId;
        private final String senderUsername;
        private final Long recipientId;
        private final String content;
        private final String timestamp;

        public ChatResponse(Long id, Long senderId, String senderUsername, Long recipientId, String content, String timestamp) {
            this.id = id;
            this.senderId = senderId;
            this.senderUsername = senderUsername;
            this.recipientId = recipientId;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}
