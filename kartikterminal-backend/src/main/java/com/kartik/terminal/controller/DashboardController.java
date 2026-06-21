package com.kartik.terminal.controller;

import com.kartik.terminal.dto.CompilerDTOs.*;
import com.kartik.terminal.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboardService;

    // ── Full dashboard: stats + recent runs + weekly activity ──
    @GetMapping
    public ResponseEntity<?> getDashboard() {
        try {
            DashboardResponse dashboard = dashboardService.getDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Execution history (paginated) ──
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<RecentExecution> history = dashboardService.getExecutionHistory(page, size);
            return ResponseEntity.ok(Map.of(
                "content",       history.getContent(),
                "totalPages",    history.getTotalPages(),
                "totalElements", history.getTotalElements(),
                "currentPage",   history.getNumber(),
                "pageSize",      history.getSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Quick stats only (lightweight call) ──
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            DashboardResponse d = dashboardService.getDashboard();
            return ResponseEntity.ok(d.getStats());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Log anti-cheat violation ──
    @PostMapping("/log-cheat")
    public ResponseEntity<?> logCheat() {
        try {
            dashboardService.logCheat();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
