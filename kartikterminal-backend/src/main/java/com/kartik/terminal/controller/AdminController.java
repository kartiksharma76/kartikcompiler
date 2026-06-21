package com.kartik.terminal.controller;

import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.ExecutionRecordRepository;
import com.kartik.terminal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ExecutionRecordRepository executionRecordRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ── All users ──
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var users = userRepository.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of(
            "users",         users.getContent().stream().map(this::safeUser).collect(java.util.stream.Collectors.toList()),
            "totalElements", users.getTotalElements(),
            "totalPages",    users.getTotalPages()
        ));
    }

    // ── Platform overview stats ──
    @GetMapping("/stats")
    public ResponseEntity<?> getPlatformStats() {
        long totalUsers   = userRepository.count();
        long activeUsers  = userRepository.countByIsActiveTrue();
        long totalRuns    = executionRecordRepository.count();
        long todayRuns    = executionRecordRepository.countPlatformExecutionsToday(
                                LocalDateTime.now().toLocalDate().atStartOfDay());
        List<Object[]> langStats = executionRecordRepository.getGlobalLanguageStats();

        return ResponseEntity.ok(Map.of(
            "totalUsers",  totalUsers,
            "activeUsers", activeUsers,
            "totalRuns",   totalRuns,
            "todayRuns",   todayRuns,
            "languageStats", langStats.stream()
                .map(r -> Map.of("language", r[0], "count", r[1]))
                .collect(java.util.stream.Collectors.toList())
        ));
    }

    // ── Deactivate/Lock a user ──
    @PostMapping("/users/{id}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setIsActive(false);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "User locked"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Disqualify a user ──
    @PostMapping("/users/{id}/disqualify")
    public ResponseEntity<?> disqualifyUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setIsDisqualified(true);
            user.setTotalPoints(0);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "User disqualified"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Unlock/Reset a user ──
    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setIsActive(true);
            user.setIsDisqualified(false);
            user.setCheatViolations(0);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "User unlocked and reset successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Unlock/Reset ALL users ──
    @PostMapping("/users/unlock-all")
    public ResponseEntity<?> unlockAllUsers() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            user.setIsActive(true);
            user.setIsDisqualified(false);
            user.setCheatViolations(0);
        }
        userRepository.saveAll(users);
        return ResponseEntity.ok(Map.of("success", true, "message", "All users unlocked and reset successfully"));
    }

    // ── Promote user to ADMIN ──
    @PatchMapping("/users/{id}/promote")
    public ResponseEntity<?> promoteUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setRole(User.Role.ADMIN);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "User promoted to ADMIN"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Create new user directly from Admin Panel ──
    @PostMapping("/users/create")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String email = payload.get("email");
        String password = payload.get("password");
        String roleStr = payload.get("role");

        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Username or Email already exists"));
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setFullName(payload.getOrDefault("fullName", username));
        newUser.setIsActive(true);
        
        try {
            newUser.setRole(User.Role.valueOf(roleStr));
        } catch(Exception e) {
            newUser.setRole(User.Role.USER);
        }

        userRepository.save(newUser);
        return ResponseEntity.ok(Map.of("success", true, "message", "User created successfully!"));
    }

    // ── Get List of Cheating/Locked Users ──
    @GetMapping("/users/cheaters")
    public ResponseEntity<?> getCheatingUsers() {
        List<Map<String, Object>> cheaters = userRepository.findAll().stream()
            .filter(u -> (u.getCheatViolations() != null && u.getCheatViolations() > 0) || 
                         (u.getIsActive() != null && !u.getIsActive()) || 
                         (u.getIsDisqualified() != null && u.getIsDisqualified()))
            .map(u -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", u.getId());
                map.put("username", u.getUsername());
                map.put("fullName", u.getFullName() != null ? u.getFullName() : u.getUsername());
                map.put("email", u.getEmail());
                map.put("cheatViolations", u.getCheatViolations() != null ? u.getCheatViolations() : 0);
                map.put("isDisqualified", u.getIsDisqualified() != null ? u.getIsDisqualified() : false);
                map.put("isActive", u.getIsActive() != null ? u.getIsActive() : true);
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(cheaters);
    }

    // ── Recent executions across all users ──
    @GetMapping("/executions")
    public ResponseEntity<?> getRecentExecutions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        var records = executionRecordRepository.findAllRecentExecutions(PageRequest.of(page, size));
        return ResponseEntity.ok(Map.of(
            "content",       records.getContent().stream().map(r -> Map.of(
                "id",       r.getId(),
                "user",     r.getUser().getUsername(),
                "language", r.getLanguage(),
                "success",  r.getSuccess(),
                "execTime", r.getExecutionTimeMs(),
                "status",   r.getStatus().name(),
                "at",       r.getExecutedAt().toString()
            )).collect(java.util.stream.Collectors.toList()),
            "totalPages",    records.getTotalPages(),
            "totalElements", records.getTotalElements()
        ));
    }

    private Map<String, Object> safeUser(User u) {
        return Map.of(
            "id",          u.getId(),
            "username",    u.getUsername(),
            "email",       u.getEmail(),
            "fullName",    u.getFullName() != null ? u.getFullName() : "",
            "role",        u.getRole().name(),
            "isActive",    u.getIsActive(),
            "totalPoints", u.getTotalPoints(),
            "executions",  u.getTotalExecutions(),
            "createdAt",   u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
        );
    }
}
