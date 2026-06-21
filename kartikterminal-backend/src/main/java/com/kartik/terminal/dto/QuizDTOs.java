package com.kartik.terminal.dto;

import lombok.Data;
import java.util.List;

public class QuizDTOs {

    @Data
    public static class QuizRequest {
        private String title;
        private String description;
        private String topic;
        private String difficulty;
        private boolean isAiGenerated;
    }

    @Data
    public static class QuestionRequest {
        private String text;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctAnswer;
        private String aiExplanation;
    }

    @Data
    public static class AIQuizGenerationRequest {
        private String topic; // e.g. Java, C++, Python
        private String difficulty; // Basic, Intermediate, Advanced
        private int count; // number of questions
    }
    
    @Data
    public static class QuizSubmissionRequest {
        private Long quizId;
        private List<AnswerRequest> answers;
    }

    @Data
    public static class AnswerRequest {
        private Long questionId;
        private String selectedOption;
    }
}
