package com.kartik.terminal.controller;

import com.kartik.terminal.dto.ProblemDTOs.*;
import com.kartik.terminal.entity.User;
import com.kartik.terminal.service.AuthService;
import com.kartik.terminal.service.ProblemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Slf4j
public class ProblemController {

    private final ProblemService problemService;
    private final AuthService authService;

    // ──────────────────────────────────────────────────────────
    // Get all problems visible to the current student / admin
    // ──────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<ProblemResponse>> getAllProblems() {
        User user = null;
        try {
            user = authService.getCurrentUser();
        } catch (Exception ignored) {}
        return ResponseEntity.ok(problemService.getProblemsForUser(user));
    }

    // ──────────────────────────────────────────────────────────
    // Get single problem by ID
    // ──────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ProblemResponse> getProblemById(@PathVariable Long id) {
        User user = null;
        try {
            user = authService.getCurrentUser();
        } catch (Exception ignored) {}
        return ResponseEntity.ok(problemService.getProblemById(id, user));
    }

    // ──────────────────────────────────────────────────────────
    // Assign / Create Problem (Platform Admin & College Admin)
    // ──────────────────────────────────────────────────────────
    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COLLEGE_ADMIN', 'FACULTY')")
    public ResponseEntity<?> assignProblem(@RequestBody ProblemRequest request) {
        try {
            User user = authService.getCurrentUser();
            ProblemResponse response = problemService.assignProblem(request, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to assign problem: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────
    // AI Problem Generator (NVIDIA NIM / Llama AI)
    // ──────────────────────────────────────────────────────────
    @PostMapping("/ai-generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COLLEGE_ADMIN', 'FACULTY')")
    public ResponseEntity<?> generateAiProblem(@RequestBody AiGenerateProblemRequest request) {
        try {
            Map<String, Object> generated = problemService.generateAiProblem(request);
            return ResponseEntity.ok(generated);
        } catch (Exception e) {
            log.error("Failed to generate AI problem: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────
    // Submit Problem Solution & Grade against 4 Hidden Test Cases
    // ──────────────────────────────────────────────────────────
    @PostMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> submitSolution(
            @PathVariable Long id,
            @RequestBody ProblemSubmissionRequest request) {
        try {
            User user = authService.getCurrentUser();
            ProblemSubmissionResponse response = problemService.submitSolution(id, request, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Submission grading failed for problem {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────
    // Delete Problem
    // ──────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COLLEGE_ADMIN', 'FACULTY')")
    public ResponseEntity<?> deleteProblem(@PathVariable Long id) {
        try {
            User user = authService.getCurrentUser();
            problemService.deleteProblem(id, user);
            return ResponseEntity.ok(Map.of("success", true, "message", "Problem successfully removed."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
