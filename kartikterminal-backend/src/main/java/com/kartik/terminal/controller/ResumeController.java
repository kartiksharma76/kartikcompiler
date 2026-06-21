package com.kartik.terminal.controller;

import com.kartik.terminal.dto.ResumeDTOs.ResumeRequest;
import com.kartik.terminal.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<?> getMyResume() {
        ResumeRequest resume = resumeService.getMyResume();
        if (resume == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(resume);
    }

    @PostMapping
    public ResponseEntity<?> saveOrUpdateResume(@RequestBody ResumeRequest request) {
        ResumeRequest updated = resumeService.saveOrUpdateResume(request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/ai-optimize")
    public ResponseEntity<?> aiOptimizeResume(@RequestBody ResumeRequest request) {
        ResumeRequest optimized = resumeService.optimizeResumeWithAI(request);
        return ResponseEntity.ok(optimized);
    }
}
