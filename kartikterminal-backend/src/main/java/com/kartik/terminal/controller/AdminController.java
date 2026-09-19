package com.kartik.terminal.controller;

import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.ExecutionRecordRepository;
import com.kartik.terminal.repository.InstitutionRepository;
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
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COLLEGE_ADMIN', 'FACULTY')")
public class AdminController {

    private final UserRepository userRepository;
    private final ExecutionRecordRepository executionRecordRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final InstitutionRepository institutionRepository;

    // ── Generate report data ──
    @GetMapping("/reports/data")
    public ResponseEntity<?> getReportData() {
        // 1. General Stats
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long totalRuns = executionRecordRepository.count();
        List<Object[]> langStats = executionRecordRepository.getGlobalLanguageStats();

        // 2. Global Leaderboard (No Institution)
        List<User> globalLeaderboard = userRepository.findTopUsersByPointsExcludingInstitutions();

        // 3. College/Company-wise Leaderboards
        List<Map<String, Object>> collegeReports = new java.util.ArrayList<>();
        List<com.kartik.terminal.entity.Institution> institutions = institutionRepository.findAll();
        for (com.kartik.terminal.entity.Institution inst : institutions) {
            List<User> instLeaderboard = userRepository.findTopUsersByPointsAndInstitution(inst);
            if (!instLeaderboard.isEmpty()) {
                Map<String, Object> instData = new java.util.HashMap<>();
                instData.put("name", inst.getName());
                instData.put("licenseKey", inst.getLicenseKey());
                instData.put("totalUsers", userRepository.countByInstitutionAndIsActiveTrue(inst));
                instData.put("totalRuns", executionRecordRepository.countInstitutionExecutionsToday(inst, LocalDateTime.now().minusYears(10))); // All-time runs
                instData.put("leaderboard", instLeaderboard.stream().map(u -> Map.of(
                    "username", u.getUsername(),
                    "fullName", u.getFullName() != null ? u.getFullName() : u.getUsername(),
                    "points", u.getTotalPoints(),
                    "executions", u.getTotalExecutions(),
                    "successRate", u.getSuccessRate()
                )).collect(java.util.stream.Collectors.toList()));
                collegeReports.add(instData);
            }
        }

        return ResponseEntity.ok(Map.of(
            "stats", Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "totalRuns", totalRuns,
                "languageStats", langStats.stream()
                    .map(r -> Map.of("language", r[0], "count", r[1]))
                    .collect(java.util.stream.Collectors.toList())
            ),
            "globalLeaderboard", globalLeaderboard.stream().map(u -> Map.of(
                "username", u.getUsername(),
                "fullName", u.getFullName() != null ? u.getFullName() : u.getUsername(),
                "points", u.getTotalPoints(),
                "executions", u.getTotalExecutions(),
                "successRate", u.getSuccessRate()
            )).collect(java.util.stream.Collectors.toList()),
            "collegeReports", collegeReports
        ));
    }

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

    private boolean isProtectedAdmin(User user) {
        if (user == null) return false;
        if ("kartiksharma768976@gmail.com".equalsIgnoreCase(user.getEmail())) return true;
        if ("kartik_admin".equalsIgnoreCase(user.getUsername())) return true;
        return user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN;
    }

    // ── Deactivate/Lock a user ──
    @PostMapping("/users/{id}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            if (isProtectedAdmin(user)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Admin accounts are protected and cannot be locked!"));
            }
            user.setIsActive(false);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "User locked"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Delete a user completely (including all dependent data) ──
    @DeleteMapping("/users/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            if (isProtectedAdmin(user)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Admin accounts are permanently protected and cannot be deleted!"));
            }
            userRepository.deleteTeamMembersByUserId(id);
            userRepository.deleteTeamsByUserId(id);
            userRepository.deleteExamProblemsByUserId(id);
            userRepository.deleteExamsByUserId(id);
            userRepository.deleteAntiCheatLogsByUserId(id);
            userRepository.deleteAiInterviewsByUserId(id);
            userRepository.deletePlagiarismReportsByUserId(id);
            userRepository.deleteProblemSubmissionsByUserId(id);
            userRepository.deleteQuizSubmissionsByUserId(id);
            userRepository.deleteAiAnalysisReportsByUserId(id);
            userRepository.deleteExecutionRecordsByUserId(id);
            userRepository.deleteChatMessagesByUserId(id);
            userRepository.deleteEducationByUserId(id);
            userRepository.deleteExperienceByUserId(id);
            userRepository.deleteProjectsByUserId(id);
            userRepository.deleteResumeByUserId(id);
            userRepository.delete(user);
            return ResponseEntity.ok(Map.of("success", true, "message", "User and all associated data deleted successfully!"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Disqualify a user ──
    @PostMapping("/users/{id}/disqualify")
    public ResponseEntity<?> disqualifyUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            if (isProtectedAdmin(user)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Admin accounts are protected and cannot be disqualified!"));
            }
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

    // ── All Registered Colleges & Status (Admin Directory) ──
    @GetMapping("/colleges")
    public ResponseEntity<?> getAllColleges() {
        List<com.kartik.terminal.entity.Institution> institutions = institutionRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (com.kartik.terminal.entity.Institution inst : institutions) {
            List<User> students = userRepository.findTopUsersByPointsAndInstitution(inst);
            long totalStudents = userRepository.countByInstitutionAndIsActiveTrue(inst);
            int totalPoints = students.stream().mapToInt(u -> u.getTotalPoints() != null ? u.getTotalPoints() : 0).sum();
            long totalRuns = students.stream().mapToLong(u -> u.getTotalExecutions() != null ? u.getTotalExecutions() : 0).sum();

            String adminUser = "—";
            if (inst.getSuperAdminId() != null) {
                adminUser = userRepository.findById(inst.getSuperAdminId())
                        .map(User::getUsername)
                        .orElse("—");
            } else if (!students.isEmpty()) {
                adminUser = students.get(0).getUsername();
            }

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", inst.getId());
            map.put("name", inst.getName());
            map.put("licenseKey", inst.getLicenseKey());
            map.put("status", inst.getStatus() != null ? inst.getStatus().name() : "PENDING");
            map.put("totalStudents", totalStudents);
            map.put("totalUsers", students.size());
            map.put("totalPoints", totalPoints);
            map.put("totalRuns", totalRuns);
            map.put("adminUser", adminUser);
            map.put("createdAt", inst.getCreatedAt() != null ? inst.getCreatedAt().toString() : "");
            list.add(map);
        }
        return ResponseEntity.ok(list);
    }

    // ── Pending College Approvals & Notifications ──
    @GetMapping("/colleges/pending")
    public ResponseEntity<?> getPendingColleges() {
        List<com.kartik.terminal.entity.Institution> pending = institutionRepository.findByStatus(com.kartik.terminal.entity.Institution.Status.PENDING);
        List<Map<String, Object>> list = pending.stream().map(inst -> {
            String adminEmail = "—";
            String adminName = "—";
            if (inst.getSuperAdminId() != null) {
                var uOpt = userRepository.findById(inst.getSuperAdminId());
                if (uOpt.isPresent()) {
                    adminEmail = uOpt.get().getEmail();
                    adminName = uOpt.get().getFullName() != null ? uOpt.get().getFullName() : uOpt.get().getUsername();
                }
            }
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", inst.getId());
            map.put("name", inst.getName());
            map.put("licenseKey", inst.getLicenseKey());
            map.put("status", "PENDING");
            map.put("adminEmail", adminEmail);
            map.put("adminName", adminName);
            map.put("createdAt", inst.getCreatedAt() != null ? inst.getCreatedAt().toString() : "");
            return map;
        }).collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(Map.of(
            "count", list.size(),
            "colleges", list
        ));
    }

    // ── Approve a College Registration ──
    @PostMapping("/colleges/{id}/approve")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> approveCollege(@PathVariable Long id) {
        return institutionRepository.findById(id).map(inst -> {
            inst.setStatus(com.kartik.terminal.entity.Institution.Status.APPROVED);
            institutionRepository.save(inst);

            // Activate all associated users of this institution
            List<User> instUsers = userRepository.findTopUsersByPointsAndInstitution(inst);
            for (User u : instUsers) {
                u.setIsActive(true);
            }
            // Also check super admin user if any
            if (inst.getSuperAdminId() != null) {
                userRepository.findById(inst.getSuperAdminId()).ifPresent(su -> {
                    su.setIsActive(true);
                    userRepository.save(su);
                });
            }
            if (!instUsers.isEmpty()) {
                userRepository.saveAll(instUsers);
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "College '" + inst.getName() + "' approved successfully! Enrolled users can now log in."
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Reject / Suspend a College Registration ──
    @PostMapping("/colleges/{id}/reject")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> rejectCollege(@PathVariable Long id) {
        return institutionRepository.findById(id).map(inst -> {
            inst.setStatus(com.kartik.terminal.entity.Institution.Status.SUSPENDED);
            institutionRepository.save(inst);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "College '" + inst.getName() + "' has been rejected / suspended."
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── Secure Student Action (Suspend, Unlock, Delete) with College Admin / Admin Password Verification ──
    @PostMapping("/colleges/students/{studentId}/secure-action")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> secureStudentAction(
            @PathVariable Long studentId,
            @RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String action = payload.get("action"); // "suspend", "unlock", "delete"

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Admin email and password are required to verify this action."
            ));
        }

        User adminUser = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseGet(() -> userRepository.findByUsername(email.toLowerCase().trim()).orElse(null));

        if (adminUser == null || !passwordEncoder.matches(password, adminUser.getPassword())) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication failed: Invalid email or password. You must provide the exact password you registered with."
            ));
        }

        User student = userRepository.findById(studentId).orElse(null);
        if (student == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student not found with ID: " + studentId));
        }

        if (isProtectedAdmin(student)) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Protected Admin accounts cannot be deleted, suspended, or modified via secure action."
            ));
        }

        boolean isPlatformAdmin = adminUser.getRole() == User.Role.ADMIN || adminUser.getRole() == User.Role.SUPER_ADMIN;
        boolean isSameCollegeAdmin = (adminUser.getRole() == User.Role.COLLEGE_ADMIN || adminUser.getRole() == User.Role.FACULTY) &&
                adminUser.getInstitution() != null && student.getInstitution() != null &&
                adminUser.getInstitution().getId().equals(student.getInstitution().getId());

        if (!isPlatformAdmin && !isSameCollegeAdmin) {
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "message", "Permission Denied: You can only manage students enrolled in your own college."
            ));
        }

        if ("delete".equalsIgnoreCase(action)) {
            userRepository.deleteTeamMembersByUserId(studentId);
            userRepository.deleteTeamsByUserId(studentId);
            userRepository.deleteExamProblemsByUserId(studentId);
            userRepository.deleteExamsByUserId(studentId);
            userRepository.deleteAntiCheatLogsByUserId(studentId);
            userRepository.deleteAiInterviewsByUserId(studentId);
            userRepository.deletePlagiarismReportsByUserId(studentId);
            userRepository.deleteProblemSubmissionsByUserId(studentId);
            userRepository.deleteQuizSubmissionsByUserId(studentId);
            userRepository.deleteAiAnalysisReportsByUserId(studentId);
            userRepository.deleteExecutionRecordsByUserId(studentId);
            userRepository.deleteChatMessagesByUserId(studentId);
            userRepository.deleteEducationByUserId(studentId);
            userRepository.deleteExperienceByUserId(studentId);
            userRepository.deleteProjectsByUserId(studentId);
            userRepository.deleteResumeByUserId(studentId);
            userRepository.delete(student);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Student @" + student.getUsername() + " has been permanently deleted after password verification."
            ));
        } else if ("suspend".equalsIgnoreCase(action)) {
            student.setIsActive(false);
            userRepository.save(student);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Student @" + student.getUsername() + " has been suspended after password verification."
            ));
        } else if ("unlock".equalsIgnoreCase(action)) {
            student.setIsActive(true);
            student.setIsDisqualified(false);
            student.setCheatViolations(0);
            userRepository.save(student);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Student @" + student.getUsername() + " has been unlocked and restored."
            ));
        }

        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid action: " + action));
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
