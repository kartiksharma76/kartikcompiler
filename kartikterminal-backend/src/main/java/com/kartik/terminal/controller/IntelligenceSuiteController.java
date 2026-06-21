package com.kartik.terminal.controller;

import com.kartik.terminal.service.IntelligenceSuiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/intelligence")
@RequiredArgsConstructor
public class IntelligenceSuiteController {

    private final IntelligenceSuiteService intelligenceSuiteService;

    @PostMapping("/visa")
    public ResponseEntity<Map<String, String>> getVisaIntelligence(@RequestBody Map<String, String> payload) {
        String targetCountry = payload.getOrDefault("targetCountry", "Germany");
        String skills = payload.getOrDefault("skills", "Java Backend Development");
        String result = intelligenceSuiteService.generateVisaIntelligence(targetCountry, skills);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/mentorship")
    public ResponseEntity<Map<String, String>> getMentorshipConnector(@RequestBody Map<String, String> payload) {
        String role = payload.getOrDefault("role", "Software Engineer");
        String targetCompany = payload.getOrDefault("targetCompany", "Google");
        String skills = payload.getOrDefault("skills", "System Design");
        String result = intelligenceSuiteService.generateMentorshipConnector(role, targetCompany, skills);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/interview")
    public ResponseEntity<Map<String, String>> getInterviewSimulator(@RequestBody Map<String, String> payload) {
        String role = payload.getOrDefault("role", "Senior Software Engineer");
        String topic = payload.getOrDefault("topic", "System Design & Concurrency");
        String result = intelligenceSuiteService.generateInterviewSimulator(role, topic);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/heatmap")
    public ResponseEntity<Map<String, String>> getTalentHeatmap(@RequestBody Map<String, String> payload) {
        String techStack = payload.getOrDefault("techStack", "Java, Kubernetes");
        String result = intelligenceSuiteService.generateTalentHeatmap(techStack);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/project-architect")
    public ResponseEntity<Map<String, String>> getProjectArchitect(@RequestBody Map<String, String> payload) {
        String projectIdea = payload.getOrDefault("projectIdea", "Realtime chat application");
        String stack = payload.getOrDefault("stack", "Spring Boot, WebSockets");
        String result = intelligenceSuiteService.generateProjectArchitect(projectIdea, stack);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/security-audit")
    public ResponseEntity<Map<String, String>> getSecurityAudit(@RequestBody Map<String, String> payload) {
        String codeSnippet = payload.getOrDefault("codeSnippet", "// Write code here");
        String result = intelligenceSuiteService.generateSecurityAudit(codeSnippet);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/skill-graph")
    public ResponseEntity<Map<String, String>> getSkillGraph(@RequestBody Map<String, String> payload) {
        String currentStack = payload.getOrDefault("currentStack", "Java, HTML/CSS");
        String targetJob = payload.getOrDefault("targetJob", "Staff AI Infrastructure Engineer");
        String result = intelligenceSuiteService.generateSkillGraph(currentStack, targetJob);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/open-source")
    public ResponseEntity<Map<String, String>> getOpenSourceHub(@RequestBody Map<String, String> payload) {
        String preferredLanguage = payload.getOrDefault("preferredLanguage", "Java");
        String difficulty = payload.getOrDefault("difficulty", "EASY");
        String result = intelligenceSuiteService.generateOpenSourceHub(preferredLanguage, difficulty);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, String>> getHackathonEventFinder(@RequestBody Map<String, String> payload) {
        String techStack = payload.getOrDefault("techStack", "Python, React");
        String domain = payload.getOrDefault("domain", "AI/ML Innovations");
        String result = intelligenceSuiteService.generateHackathonEventFinder(techStack, domain);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/career-multiplier")
    public ResponseEntity<Map<String, String>> getCareerMultiplier(@RequestBody Map<String, String> payload) {
        String currentStack = payload.getOrDefault("currentStack", "Java Spring");
        String result = intelligenceSuiteService.generateCareerMultiplier(currentStack);
        return ResponseEntity.ok(Map.of("result", result));
    }
}
