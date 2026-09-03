package com.kartik.terminal.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/proctor")
@RequiredArgsConstructor
@Slf4j
public class ProctorController {

    private static final Map<Long, ProctorFrame> activeFeeds = new ConcurrentHashMap<>();
    private static final long FEED_TIMEOUT_MS = 6000; // 6 seconds before marked offline

    @PostMapping("/stream-frame")
    public ResponseEntity<?> receiveFrame(@RequestBody FramePayload payload) {
        if (payload.getUserId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "UserId required"));
        }

        ProctorFrame frame = new ProctorFrame();
        frame.setUserId(payload.getUserId());
        frame.setUsername(payload.getUsername() != null ? payload.getUsername() : "Student #" + payload.getUserId());
        frame.setFullName(payload.getFullName() != null ? payload.getFullName() : frame.getUsername());
        frame.setImageData(payload.getFrame());
        frame.setViolations(payload.getViolations() != null ? payload.getViolations() : 0);
        frame.setSecurityMode(payload.getSecurityMode() != null ? payload.getSecurityMode() : true);
        frame.setLastUpdated(System.currentTimeMillis());

        activeFeeds.put(payload.getUserId(), frame);
        return ResponseEntity.ok(Map.of("status", "ok", "activeStudents", activeFeeds.size()));
    }

    @GetMapping("/active-feeds")
    public ResponseEntity<?> getActiveFeeds() {
        long now = System.currentTimeMillis();
        // Remove stale feeds
        activeFeeds.entrySet().removeIf(entry -> (now - entry.getValue().getLastUpdated()) > FEED_TIMEOUT_MS);

        List<ProctorFrame> list = new ArrayList<>(activeFeeds.values());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/stop-feed/{userId}")
    public ResponseEntity<?> stopFeed(@PathVariable Long userId) {
        activeFeeds.remove(userId);
        return ResponseEntity.ok(Map.of("status", "stopped"));
    }

    @Data
    public static class FramePayload {
        private Long userId;
        private String username;
        private String fullName;
        private String frame; // base64 data url
        private Integer violations;
        private Boolean securityMode;
    }

    @Data
    public static class ProctorFrame {
        private Long userId;
        private String username;
        private String fullName;
        private String imageData;
        private Integer violations;
        private Boolean securityMode;
        private Long lastUpdated;
    }
}
