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
    private final com.kartik.terminal.repository.InstitutionRepository institutionRepository;

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
                .cheatViolations(user.getCheatViolations() != null ? user.getCheatViolations() : 0)
                .isActive(user.getIsActive() != null ? user.getIsActive() : true)
                .isDisqualified(user.getIsDisqualified() != null ? user.getIsDisqualified() : false)
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

    // ========== COLLEGE LEADERBOARD ==========
    @Transactional(readOnly = true)
    public List<CollegeLeaderboardEntry> getCollegeLeaderboard() {
        List<com.kartik.terminal.entity.Institution> institutions = institutionRepository.findByStatus(com.kartik.terminal.entity.Institution.Status.APPROVED);
        List<CollegeLeaderboardEntry> list = new ArrayList<>();

        for (com.kartik.terminal.entity.Institution inst : institutions) {
            List<User> students = userRepository.findTopUsersByPointsAndInstitution(inst);
            long totalStudents = userRepository.countByInstitutionAndIsActiveTrue(inst);
            int totalPoints = students.stream().mapToInt(u -> u.getTotalPoints() != null ? u.getTotalPoints() : 0).sum();
            long totalExecs = students.stream().mapToLong(u -> u.getTotalExecutions() != null ? u.getTotalExecutions() : 0).sum();

            double avgSuccess = 0.0;
            if (!students.isEmpty()) {
                double totalSuccess = students.stream().mapToDouble(User::getSuccessRate).sum();
                avgSuccess = Math.round((totalSuccess / students.size()) * 10.0) / 10.0;
            }

            String topCoder = "—";
            int topPoints = 0;
            if (!students.isEmpty()) {
                User best = students.get(0);
                topCoder = best.getFullName() != null && !best.getFullName().isBlank() ? best.getFullName() : best.getUsername();
                topPoints = best.getTotalPoints() != null ? best.getTotalPoints() : 0;
            }

            String regDate = inst.getCreatedAt() != null ? inst.getCreatedAt().toLocalDate().toString() : "Recent";

            list.add(CollegeLeaderboardEntry.builder()
                    .id(inst.getId())
                    .institutionId(inst.getId())
                    .name(inst.getName())
                    .licenseKey(inst.getLicenseKey())
                    .status(inst.getStatus() != null ? inst.getStatus().name() : "APPROVED")
                    .totalStudents(totalStudents)
                    .totalPoints(totalPoints)
                    .totalExecutions(totalExecs)
                    .avgSuccessRate(avgSuccess)
                    .topCoderName(topCoder)
                    .topCoderPoints(topPoints)
                    .registeredAt(regDate)
                    .build());
        }

        list.sort((a, b) -> {
            int cmp = Integer.compare(b.getTotalPoints(), a.getTotalPoints());
            if (cmp != 0) return cmp;
            return Long.compare(b.getTotalExecutions(), a.getTotalExecutions());
        });

        for (int i = 0; i < list.size(); i++) {
            list.get(i).setRank(i + 1);
        }

        return list;
    }

    // ========== GET DETAILED COLLEGE LEADERBOARD & PERFORMANCE ==========
    @Transactional(readOnly = true)
    public Map<String, Object> getCollegeDetails(Long institutionId) {
        com.kartik.terminal.entity.Institution inst = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new RuntimeException("College not found with id: " + institutionId));

        List<User> students = userRepository.findTopUsersByPointsAndInstitution(inst);
        long totalStudents = userRepository.countByInstitutionAndIsActiveTrue(inst);
        int totalPoints = students.stream().mapToInt(u -> u.getTotalPoints() != null ? u.getTotalPoints() : 0).sum();
        long totalExecs = students.stream().mapToLong(u -> u.getTotalExecutions() != null ? u.getTotalExecutions() : 0).sum();

        double avgSuccess = 0.0;
        if (!students.isEmpty()) {
            double totalSuccess = students.stream().mapToDouble(User::getSuccessRate).sum();
            avgSuccess = Math.round((totalSuccess / students.size()) * 10.0) / 10.0;
        }

        List<Map<String, Object>> studentList = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            User u = students.get(i);
            Map<String, Object> smap = new LinkedHashMap<>();
            smap.put("rank", i + 1);
            smap.put("id", u.getId());
            smap.put("username", u.getUsername());
            smap.put("fullName", u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername());
            smap.put("email", u.getEmail());
            smap.put("avatarUrl", u.getAvatarUrl());
            smap.put("totalPoints", u.getTotalPoints() != null ? u.getTotalPoints() : 0);
            smap.put("totalExecutions", u.getTotalExecutions() != null ? u.getTotalExecutions() : 0);
            smap.put("successfulExecutions", u.getSuccessfulExecutions() != null ? u.getSuccessfulExecutions() : 0);
            smap.put("successRate", Math.round(u.getSuccessRate() * 10.0) / 10.0);
            smap.put("favoriteLanguage", u.getFavoriteLanguage());
            smap.put("tier", getTier(u.getTotalPoints() != null ? u.getTotalPoints() : 0));
            smap.put("cheatViolations", u.getCheatViolations() != null ? u.getCheatViolations() : 0);
            smap.put("isActive", u.getIsActive() != null ? u.getIsActive() : true);
            smap.put("isDisqualified", u.getIsDisqualified() != null ? u.getIsDisqualified() : false);
            studentList.add(smap);
        }

        Pageable recentPage = PageRequest.of(0, 30);
        List<ExecutionRecord> recentRuns = executionRecordRepository.findRecentExecutionsByInstitution(inst, recentPage);
        List<Map<String, Object>> runsList = recentRuns.stream().map(r -> {
            Map<String, Object> rmap = new LinkedHashMap<>();
            rmap.put("id", r.getId());
            rmap.put("userId", r.getUser() != null ? r.getUser().getId() : null);
            rmap.put("username", r.getUser() != null ? r.getUser().getUsername() : "anonymous");
            rmap.put("fullName", r.getUser() != null && r.getUser().getFullName() != null ? r.getUser().getFullName() : (r.getUser() != null ? r.getUser().getUsername() : ""));
            rmap.put("language", r.getLanguage());
            rmap.put("code", r.getCode() != null ? r.getCode() : "");
            rmap.put("success", r.getSuccess());
            rmap.put("status", r.getStatus() != null ? r.getStatus().name() : "SUCCESS");
            rmap.put("executionTimeMs", r.getExecutionTimeMs());
            rmap.put("points", r.getPoints());
            rmap.put("executedAt", r.getExecutedAt() != null ? r.getExecutedAt().toString() : "");
            return rmap;
        }).collect(Collectors.toList());

        Map<String, Object> instMap = new LinkedHashMap<>();
        instMap.put("id", inst.getId());
        instMap.put("name", inst.getName() != null ? inst.getName() : "");
        instMap.put("licenseKey", inst.getLicenseKey() != null ? inst.getLicenseKey() : "");
        instMap.put("status", inst.getStatus() != null ? inst.getStatus().name() : "APPROVED");
        instMap.put("createdAt", inst.getCreatedAt() != null ? inst.getCreatedAt().toString() : "");

        Map<String, Object> statsMap = new LinkedHashMap<>();
        statsMap.put("totalStudents", totalStudents);
        statsMap.put("totalPoints", totalPoints);
        statsMap.put("totalExecutions", totalExecs);
        statsMap.put("avgSuccessRate", avgSuccess);

        Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.put("institution", instMap);
        resultMap.put("stats", statsMap);
        resultMap.put("students", studentList);
        resultMap.put("recentExecutions", runsList);

        return resultMap;
    }

    // ========== LEADERBOARD ==========
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard() {
        User currentUser = null;
        try {
            currentUser = authService.getCurrentUser();
        } catch (Exception ignored) {}
        
        List<User> topCodersRaw;
        List<User> topQuizRaw;
        List<User> topAIRaw;
        long totalUsers;
        long todayExecutions;
        String companyName = "Global Public Arena";

        LocalDateTime startOfToday = LocalDateTime.now().toLocalDate().atStartOfDay();

        if (currentUser != null && currentUser.getInstitution() != null) {
            // Company/institution specific leaderboard
            companyName = currentUser.getInstitution().getName();
            topCodersRaw = userRepository.findTopUsersByPointsAndInstitution(currentUser.getInstitution());
            topQuizRaw = userRepository.findTopUsersByQuizPointsAndInstitution(currentUser.getInstitution());
            topAIRaw = userRepository.findTopUsersByAiPointsAndInstitution(currentUser.getInstitution());
            totalUsers = userRepository.countByInstitutionAndIsActiveTrue(currentUser.getInstitution());
            todayExecutions = executionRecordRepository.countInstitutionExecutionsToday(currentUser.getInstitution(), startOfToday);
        } else {
            // General/global leaderboard (excluding institutional users)
            topCodersRaw = userRepository.findTopUsersByPointsExcludingInstitutions();
            topQuizRaw = userRepository.findTopUsersByQuizPointsExcludingInstitutions();
            topAIRaw = userRepository.findTopUsersByAiPointsExcludingInstitutions();
            totalUsers = userRepository.countByInstitutionIsNullAndIsActiveTrue();
            todayExecutions = executionRecordRepository.countPlatformExecutionsToday(startOfToday);
        }

        List<LeaderboardEntry> topCoders = buildLeaderboardEntries(topCodersRaw, currentUser, "coding");
        List<LeaderboardEntry> topQuizTakers = buildLeaderboardEntries(topQuizRaw, currentUser, "quiz");
        List<LeaderboardEntry> topAIUsers = buildLeaderboardEntries(topAIRaw, currentUser, "ai");
        List<CollegeLeaderboardEntry> collegeLeaderboard = getCollegeLeaderboard();

        // Current user rank (if authenticated)
        LeaderboardEntry currentUserEntry = null;
        if (currentUser != null) {
            int currentUserRankPos = getUserRank(currentUser);
            currentUserEntry = LeaderboardEntry.builder()
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
        }

        return LeaderboardResponse.builder()
                .topCoders(topCoders)
                .topQuizTakers(topQuizTakers)
                .topAIUsers(topAIUsers)
                .collegeLeaderboard(collegeLeaderboard)
                .currentUserRank(currentUserEntry)
                .totalUsers(totalUsers)
                .totalExecutionsToday(todayExecutions)
                .companyName(companyName)
                .build();
    }

    private List<LeaderboardEntry> buildLeaderboardEntries(List<User> topUsers, User currentUser, String type) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < Math.min(topUsers.size(), 50); i++) {
            User u = topUsers.get(i);
            int points;
            if ("quiz".equals(type)) points = u.getQuizPoints() != null ? u.getQuizPoints() : 0;
            else if ("ai".equals(type)) points = u.getAiPoints() != null ? u.getAiPoints() : 0;
            else points = u.getTotalPoints() != null ? u.getTotalPoints() : 0;
            
            boolean isMe = currentUser != null && u.getId().equals(currentUser.getId());

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
                    .isCurrentUser(isMe)
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
        int currentViolations = (user.getCheatViolations() != null ? user.getCheatViolations() : 0) + 1;
        user.setCheatViolations(currentViolations);
        if (currentViolations >= 3) {
            user.setIsActive(false);
            user.setIsDisqualified(true);
        }
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
        List<User> allUsers;
        if (user.getInstitution() != null) {
            allUsers = userRepository.findTopUsersByPointsAndInstitution(user.getInstitution());
        } else {
            allUsers = userRepository.findTopUsersByPointsExcludingInstitutions();
        }
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
