package com.kartik.terminal.service;

import com.kartik.terminal.dto.CompilerDTOs;
import com.kartik.terminal.dto.CompilerDTOs.*;
import com.kartik.terminal.entity.ExecutionRecord;
import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.ExecutionRecordRepository;
import com.kartik.terminal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final ExecutionRecordRepository executionRecordRepository;
    private final AuthService authService;

    // ========== DASHBOARD ==========
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        User user = authService.getCurrentUser();

        // Get recent executions
        Pageable recentPage = PageRequest.of(0, 10);
        Page<ExecutionRecord> recentRaw = executionRecordRepository
                .findByUserIdOrderByExecutedAtDesc(user.getId(), recentPage);

        List<RecentExecution> recentExecutions = recentRaw.getContent().stream()
                .map(this::mapToRecentExecution)
                .collect(Collectors.toList());

        // Language breakdown
        Map<String, Long> languageBreakdown = buildLanguageBreakdown(user.getId());

        // Weekly activity (last 7 days)
        List<DailyActivity> weeklyActivity = buildWeeklyActivity(user.getId());

        // Today's execution count
        long todayCount = executionRecordRepository.countTodayExecutions(
                user.getId(), LocalDateTime.now().toLocalDate().atStartOfDay());

        // User rank
        int rank = getUserRank(user);

        UserStats stats = UserStats.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .totalExecutions(user.getTotalExecutions())
                .successfulExecutions(user.getSuccessfulExecutions())
                .totalPoints(user.getTotalPoints())
                .successRate(Math.round(user.getSuccessRate() * 10.0) / 10.0)
                .avgExecutionTime(Math.round(user.getAverageExecutionTime() * 10.0) / 10.0)
                .favoriteLanguage(user.getFavoriteLanguage())
                .memberSince(user.getCreatedAt())
                .lastActive(user.getLastLoginAt())
                .todayExecutions((int) todayCount)
                .role(user.getRole().name())
                .cheatViolations(user.getCheatViolations())
                .build();

        return DashboardResponse.builder()
                .stats(stats)
                .recentExecutions(recentExecutions)
                .languageBreakdown(languageBreakdown)
                .weeklyActivity(weeklyActivity)
                .currentStreak(calculateStreak(user.getId()))
                .rank(rank)
                .tier(getTier(user.getTotalPoints()))
                .totalCheatViolations(userRepository.findAll().stream().mapToLong(User::getCheatViolations).sum())
                .build();
    }

    // ========== LEADERBOARD ==========
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard() {
        User currentUser = authService.getCurrentUser();
        
        List<User> topCodersRaw = userRepository.findTopUsersByPoints();
        List<User> topQuizRaw = userRepository.findTopUsersByQuizPoints();
        List<User> topAIRaw = userRepository.findTopUsersByAiPoints();

        // Get today's total executions
        long todayExecutions = executionRecordRepository.countPlatformExecutionsToday(
                LocalDateTime.now().toLocalDate().atStartOfDay());

        List<LeaderboardEntry> topCoders = buildLeaderboardEntries(topCodersRaw, currentUser, "coding");
        List<LeaderboardEntry> topQuizTakers = buildLeaderboardEntries(topQuizRaw, currentUser, "quiz");
        List<LeaderboardEntry> topAIUsers = buildLeaderboardEntries(topAIRaw, currentUser, "ai");

        // Current user rank (based on their coding points by default for the main rank display)
        int currentUserRankPos = getUserRank(currentUser);
        LeaderboardEntry currentUserEntry = LeaderboardEntry.builder()
                .rank(currentUserRankPos)
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .fullName(currentUser.getFullName())
                .totalPoints(currentUser.getTotalPoints())
                .totalExecutions(currentUser.getTotalExecutions())
                .successfulExecutions(currentUser.getSuccessfulExecutions())
                .successRate(Math.round(currentUser.getSuccessRate() * 10.0) / 10.0)
                .favoriteLanguage(currentUser.getFavoriteLanguage())
                .tier(getTier(currentUser.getTotalPoints()))
                .isCurrentUser(true)
                .cheatViolations(currentUser.getCheatViolations())
                .isActive(currentUser.getIsActive())
                .isDisqualified(currentUser.getIsDisqualified())
                .build();

        return LeaderboardResponse.builder()
                .topCoders(topCoders)
                .topQuizTakers(topQuizTakers)
                .topAIUsers(topAIUsers)
                .currentUserRank(currentUserEntry)
                .totalUsers(userRepository.countByIsActiveTrue())
                .totalExecutionsToday(todayExecutions)
                .build();
    }

    private List<LeaderboardEntry> buildLeaderboardEntries(List<User> topUsers, User currentUser, String type) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < Math.min(topUsers.size(), 50); i++) {
            User u = topUsers.get(i);
            int points;
            if ("quiz".equals(type)) points = u.getQuizPoints();
            else if ("ai".equals(type)) points = u.getAiPoints();
            else points = u.getTotalPoints();
            
            entries.add(LeaderboardEntry.builder()
                    .rank(i + 1)
                    .userId(u.getId())
                    .username(u.getUsername())
                    .fullName(u.getFullName())
                    .avatarUrl(u.getAvatarUrl())
                    .totalPoints(points)
                    .totalExecutions(u.getTotalExecutions())
                    .successfulExecutions(u.getSuccessfulExecutions())
                    .successRate(Math.round(u.getSuccessRate() * 10.0) / 10.0)
                    .favoriteLanguage(u.getFavoriteLanguage())
                    .tier(getTier(points))
                    .isCurrentUser(u.getId().equals(currentUser.getId()))
                    .cheatViolations(u.getCheatViolations())
                    .isActive(u.getIsActive())
                    .isDisqualified(u.getIsDisqualified())
                    .build());
        }
        return entries;
    }

    // ========== EXECUTION HISTORY ==========
    @Transactional(readOnly = true)
    public Page<RecentExecution> getExecutionHistory(int page, int size) {
        User user = authService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return executionRecordRepository
                .findByUserIdOrderByExecutedAtDesc(user.getId(), pageable)
                .map(this::mapToRecentExecution);
    }

    // ========== ANTI CHEAT ==========
    @Transactional
    public void logCheat() {
        User user = authService.getCurrentUser();
        user.setCheatViolations(user.getCheatViolations() + 1);
        userRepository.save(user);
    }

    // ========== PRIVATE HELPERS ==========
    private RecentExecution mapToRecentExecution(ExecutionRecord record) {
        String preview = record.getCode() != null
                ? record.getCode().substring(0, Math.min(80, record.getCode().length())).replace("\n", " ")
                : "";
        return RecentExecution.builder()
                .id(record.getId())
                .language(record.getLanguage())
                .success(record.getSuccess())
                .executionTimeMs(record.getExecutionTimeMs())
                .points(record.getPoints())
                .status(record.getStatus().name())
                .executedAt(record.getExecutedAt())
                .codePreview(preview)
                .title(record.getTitle())
                .build();
    }

    private Map<String, Long> buildLanguageBreakdown(Long userId) {
        List<Object[]> raw = executionRecordRepository.countByLanguageForUser(userId);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : raw) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }

    private List<DailyActivity> buildWeeklyActivity(Long userId) {
        List<DailyActivity> activity = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            long count = executionRecordRepository.countTodayExecutions(userId, start);
            long success = executionRecordRepository.countByUserIdAndSuccessTrue(userId);
            activity.add(DailyActivity.builder()
                    .date(date.format(fmt))
                    .executions(count)
                    .successCount(Math.min(success, count))
                    .build());
        }
        return activity;
    }

    private int getUserRank(User user) {
        List<User> allUsers = userRepository.findTopUsersByPoints();
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getId().equals(user.getId())) {
                return i + 1;
            }
        }
        return allUsers.size() + 1;
    }

    private int calculateStreak(Long userId) {
        List<ExecutionRecord> records = executionRecordRepository
                .findByUserIdOrderByExecutedAtDesc(userId);
        if (records.isEmpty()) return 0;

        Set<LocalDate> executionDays = records.stream()
                .map(r -> r.getExecutedAt().toLocalDate())
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate check = LocalDate.now();
        while (executionDays.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }
        return streak;
    }

    private String getTier(int points) {
        return CompilerDTOs.getTier(points);
    }
}
