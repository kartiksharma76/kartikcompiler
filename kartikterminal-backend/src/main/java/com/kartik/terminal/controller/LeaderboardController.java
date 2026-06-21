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
                "totalUsers", 0,
                "totalExecutionsToday", 0,
                "error", "Login to see your rank"
            ));
        }
    }
}
