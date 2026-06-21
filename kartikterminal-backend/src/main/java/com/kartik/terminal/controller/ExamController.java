package com.kartik.terminal.controller;

import com.kartik.terminal.entity.Exam;
import com.kartik.terminal.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @PostMapping
    public ResponseEntity<Exam> createExam(@RequestBody Exam exam) {
        return ResponseEntity.ok(examService.createExam(exam));
    }

    @GetMapping("/faculty/{facultyId}")
    public ResponseEntity<List<Exam>> getFacultyExams(@PathVariable Long facultyId) {
        return ResponseEntity.ok(examService.getExamsByFaculty(facultyId));
    }
}
