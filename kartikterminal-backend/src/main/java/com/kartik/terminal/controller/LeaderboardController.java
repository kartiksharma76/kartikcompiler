package com.kartik.terminal.controller;

import com.kartik.terminal.dto.CompilerDTOs.*;
import com.kartik.terminal.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final DashboardService dashboardService;

    // Public leaderboard — no auth needed to view top users
    @GetMapping
    public ResponseEntity<?> getLeaderboard() {
        try {
            LeaderboardResponse leaderboard = dashboardService.getLeaderboard();
            return ResponseEntity.ok(leaderboard);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "topCoders", java.util.List.of(),
                "topQuizTakers", java.util.List.of(),
                "topAIUsers", java.util.List.of(),
                "collegeLeaderboard", java.util.List.of(),
                "totalUsers", 0,
                "totalExecutionsToday", 0,
                "error", "Login to see your rank"
            ));
        }
    }

    // Public college-wise leaderboard
    @GetMapping("/colleges")
    public ResponseEntity<?> getCollegeLeaderboard() {
        try {
            var collegeLeaderboard = dashboardService.getCollegeLeaderboard();
            return ResponseEntity.ok(collegeLeaderboard);
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.List.of());
        }
    }

    // Public individual college leaderboard & execution details
    @GetMapping("/colleges/{id}")
    public ResponseEntity<?> getCollegeDetails(@PathVariable Long id) {
        try {
            var details = dashboardService.getCollegeDetails(id);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // Public student executions & anti-cheat audit
    @GetMapping("/students/{userId}/executions")
    public ResponseEntity<?> getStudentExecutions(@PathVariable Long userId) {
        try {
            var data = dashboardService.getStudentExecutionsAndAntiCheat(userId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
