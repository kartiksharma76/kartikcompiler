package com.kartik.terminal.controller;

import com.kartik.terminal.dto.QuizDTOs.*;
import com.kartik.terminal.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // === Admin / Generators ===

    @PostMapping("/admin/create")
    public ResponseEntity<?> createQuiz(@RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.createQuiz(request));
    }

    @PostMapping("/admin/{quizId}/questions")
    public ResponseEntity<?> addQuestion(@PathVariable Long quizId, @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(quizService.addQuestion(quizId, request));
    }

    @PostMapping("/ai/generate")
    public ResponseEntity<?> generateAIQuiz(@RequestBody AIQuizGenerationRequest request) {
        return ResponseEntity.ok(quizService.generateQuizWithAI(request));
    }

    // === User Facing ===

    @GetMapping
    public ResponseEntity<?> getQuizzes() {
        return ResponseEntity.ok(quizService.getAvailableQuizzes());
    }

    @GetMapping("/{quizId}/questions")
    public ResponseEntity<?> getQuestions(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getQuizQuestions(quizId));
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody QuizSubmissionRequest request) {
        return ResponseEntity.ok(quizService.submitQuiz(request));
    }

    @GetMapping("/submission/{submissionId}/ai-review")
    public ResponseEntity<?> getQuizAiReview(@PathVariable Long submissionId) {
        return ResponseEntity.ok(java.util.Map.of("review", quizService.getQuizAiReview(submissionId)));
    }

    @GetMapping("/questions/{questionId}/hint")
    public ResponseEntity<?> getAIHint(@PathVariable Long questionId) {
        return ResponseEntity.ok(quizService.getAiHint(questionId));
    }
}
