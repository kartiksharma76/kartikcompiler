package com.kartik.terminal.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CompilerDTOs {

    // --- Code Execution Request ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeRequest {

        @NotBlank(message = "Code cannot be empty")
        @Size(max = 50000, message = "Code too large (max 50KB)")
        private String code;

        @NotBlank(message = "Language is required")
        @Pattern(regexp = "^(java|python|cpp|c|js|go|mysql|ts)$", message = "Unsupported language")
        private String language;

        @Size(max = 10000, message = "Input too large")
        private String input;

        // Optional: save with title
        private String title;
    }

    // --- Execution Response ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExecutionResponse {
        private String output;
        private String error;
        private boolean success;
        private long executionTimeMs;
        private String language;
        private int points;
        private String status;
        private LocalDateTime executedAt;
        private Long recordId;
    }

    // --- Dashboard Response ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardResponse {
        private UserStats stats;
        private List<RecentExecution> recentExecutions;
        private Map<String, Long> languageBreakdown;
        private List<DailyActivity> weeklyActivity;
        private int currentStreak;
        private int rank;
        private String tier;
        private long totalCheatViolations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStats {
        private Long userId;
        private String username;
        private String fullName;
        private String avatarUrl;
        private int totalExecutions;
        private int successfulExecutions;
        private int totalPoints;
        private double successRate;
        private double avgExecutionTime;
        private String favoriteLanguage;
        private LocalDateTime memberSince;
        private LocalDateTime lastActive;
        private int todayExecutions;
        private String role;
        private int cheatViolations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentExecution {
        private Long id;
        private String language;
        private boolean success;
        private long executionTimeMs;
        private int points;
        private String status;
        private LocalDateTime executedAt;
        private String codePreview;
        private String title;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyActivity {
        private String date;
        private long executions;
        private long successCount;
    }

    // --- Leaderboard Response ---
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaderboardEntry {
        private int rank;
        private Long userId;
        private String username;
        private String fullName;
        private String avatarUrl;
        private int totalPoints;
        private int totalExecutions;
        private int successfulExecutions;
        private double successRate;
        private String favoriteLanguage;
        private String tier;
        @com.fasterxml.jackson.annotation.JsonProperty("isCurrentUser")
        private boolean isCurrentUser;
        private int cheatViolations;
        @com.fasterxml.jackson.annotation.JsonProperty("isActive")
        private boolean isActive;
        @com.fasterxml.jackson.annotation.JsonProperty("isDisqualified")
        private boolean isDisqualified;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LeaderboardResponse {
        private List<LeaderboardEntry> topCoders;
        private List<LeaderboardEntry> topQuizTakers;
        private List<LeaderboardEntry> topAIUsers;
        
        private LeaderboardEntry currentUserRank;
        private long totalUsers;
        private long totalExecutionsToday;
    }

    // Helper: Get tier based on points
    public static String getTier(int points) {
        if (points >= 1000) return "DIAMOND";
        if (points >= 500) return "PLATINUM";
        if (points >= 250) return "GOLD";
        if (points >= 100) return "SILVER";
        return "BRONZE";
    }
}
