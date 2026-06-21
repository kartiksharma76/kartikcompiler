package com.kartik.terminal.controller;

import com.kartik.terminal.dto.CompilerDTOs.*;
import com.kartik.terminal.service.CompilerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles code execution requests from compiler.html
 *
 * POST /compiler/run  →  run code, save to DB, return output
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class CompilerController {

    private final CompilerService compilerService;

    // ──────────────────────────────────────────────────────────
    //  Main execute endpoint — called by compiler.html fetch()
    //  Requires JWT Authorization header
    // ──────────────────────────────────────────────────────────
    @PostMapping("/compiler/run")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> runCode(@Valid @RequestBody CodeRequest request) {
        try {
            ExecutionResponse result = compilerService.executeCode(request);

            // Return plain text if output is present (backward compat with compiler.html)
            // compiler.html reads response as text and displays in terminal
            if (result.isSuccess()) {
                return ResponseEntity.ok(result.getOutput());
            } else {
                // Return error output — compiler.html checks for "error" keyword
                String errorText = result.getError().isBlank()
                        ? "Runtime error (exit code non-zero)"
                        : result.getError();
                return ResponseEntity.ok(errorText);
            }
        } catch (Exception e) {
            log.error("Execution failed: {}", e.getMessage());
            return ResponseEntity.ok("Internal Error: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────
    //  Rich JSON response endpoint (for dashboard / history UI)
    // ──────────────────────────────────────────────────────────
    @PostMapping("/api/compiler/run")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> runCodeJson(@Valid @RequestBody CodeRequest request) {
        try {
            ExecutionResponse result = compilerService.executeCode(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Execution failed: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────────────────
    //  Supported languages list
    // ──────────────────────────────────────────────────────────
    @GetMapping("/api/compiler/languages")
    public ResponseEntity<?> getSupportedLanguages() {
        return ResponseEntity.ok(Map.of(
            "languages", java.util.List.of(
                Map.of("id", "java",   "name", "Java 17",    "extension", "java"),
                Map.of("id", "python", "name", "Python 3.10","extension", "py"),
                Map.of("id", "cpp",    "name", "C++ 20",     "extension", "cpp"),
                Map.of("id", "c",      "name", "C 11",       "extension", "c"),
                Map.of("id", "js",     "name", "Node.js",    "extension", "js"),
                Map.of("id", "go",     "name", "Go 1.21",    "extension", "go")
            )
        ));
    }
}
