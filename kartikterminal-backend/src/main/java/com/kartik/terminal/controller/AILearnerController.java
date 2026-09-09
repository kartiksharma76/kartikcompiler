package com.kartik.terminal.controller;

import com.kartik.terminal.service.AILearnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ailearner")
@RequiredArgsConstructor
public class AILearnerController {

    private final AILearnerService aiLearnerService;

    // ── 1. Voice Coding Engine (Image 1) ──
    @PostMapping("/voice-code")
    public ResponseEntity<Map<String, Object>> generateVoiceCode(@RequestBody Map<String, Object> payload) {
        String prompt = payload.getOrDefault("prompt", "Create a Spring Boot REST API for student management with JWT authentication").toString();
        String language = payload.getOrDefault("language", "Java").toString();
        String mode = payload.getOrDefault("mode", "Generate Code").toString();
        boolean autoRun = Boolean.parseBoolean(payload.getOrDefault("autoRun", "true").toString());

        return ResponseEntity.ok(aiLearnerService.processVoiceCoding(prompt, language, mode, autoRun));
    }

    @PostMapping("/voice-action")
    public ResponseEntity<Map<String, Object>> runVoiceAction(@RequestBody Map<String, String> payload) {
        String action = payload.getOrDefault("action", "explain");
        String code = payload.getOrDefault("code", "");
        String language = payload.getOrDefault("language", "Java");
        String targetLanguage = payload.getOrDefault("targetLanguage", "C++");

        return ResponseEntity.ok(aiLearnerService.runVoiceAction(action, code, language, targetLanguage));
    }

    // ── 2. Real-Time Collaborate Session (Image 2) ──
    @GetMapping("/collaborate/session")
    public ResponseEntity<Map<String, Object>> getCollaborateSession(@RequestParam(defaultValue = "ecommerce-api") String room) {
        return ResponseEntity.ok(aiLearnerService.getCollaborateRoomData(room));
    }

    // ── 3. AI Socratic Tutor Mode (Image 3) ──
    @PostMapping("/tutor/interact")
    public ResponseEntity<Map<String, Object>> interactWithTutor(@RequestBody Map<String, Object> payload) {
        String problemTitle = payload.getOrDefault("problemTitle", "1. Two Sum").toString();
        String problemDescription = payload.getOrDefault("problemDescription", "Given an array of integers nums and an integer target, return indices of two numbers that add up to target.").toString();
        String userCode = payload.getOrDefault("userCode", "").toString();
        String userMessage = payload.getOrDefault("userMessage", "").toString();
        int currentStep = payload.containsKey("currentStep") ? Integer.parseInt(payload.get("currentStep").toString()) : 1;
        List<Map<String, String>> chatHistory = (List<Map<String, String>>) payload.getOrDefault("chatHistory", List.of());

        return ResponseEntity.ok(aiLearnerService.processTutorInteraction(problemTitle, problemDescription, userCode, userMessage, chatHistory, currentStep));
    }

    // ── 4. AI Code-To-Video Explainer (Image 4) ──
    @PostMapping("/code-to-video")
    public ResponseEntity<Map<String, Object>> generateCodeToVideo(@RequestBody Map<String, String> payload) {
        String code = payload.getOrDefault("code", "public class TwoSum { ... }");
        String language = payload.getOrDefault("language", "Java");
        String style = payload.getOrDefault("style", "Modern (Animated)");
        String voice = payload.getOrDefault("voice", "English (US) - Natural");

        return ResponseEntity.ok(aiLearnerService.generateCodeToVideo(code, language, style, voice));
    }
}
