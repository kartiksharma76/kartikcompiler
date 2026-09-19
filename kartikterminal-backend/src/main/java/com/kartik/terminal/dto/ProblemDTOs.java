package com.kartik.terminal.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProblemDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCaseDTO {
        private Long id;
        private String inputData;
        private String expectedOutput;
        private Boolean isHidden;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProblemRequest {
        private String title;
        private String description;
        private String difficulty; // EASY, MEDIUM, HARD
        private Integer points;
        private String tags;
        private String sampleInput;
        private String sampleOutput;
        private String starterCode;
        private Long targetInstitutionId; // For platform admin to target specific college, or null for global
        private Boolean assignGlobally; // true = global to all students
        @Builder.Default
        private List<TestCaseDTO> testCases = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProblemResponse {
        private Long id;
        private String title;
        private String description;
        private String difficulty;
        private Integer points;
        private String tags;
        private String sampleInput;
        private String sampleOutput;
        private String starterCode;
        private Boolean isGlobal;
        private Long institutionId;
        private String institutionName;
        private String createdByUsername;
        private LocalDateTime createdAt;
        private Boolean isSolved;
        private Integer totalTestCasesCount;
        private Integer hiddenTestCasesCount;
        @Builder.Default
        private List<TestCaseDTO> publicTestCases = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiGenerateProblemRequest {
        private String topic;
        private String difficulty; // EASY, MEDIUM, HARD
        private String language;
        private Long targetInstitutionId;
        private Boolean assignGlobally;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProblemSubmissionRequest {
        private String language;
        private String code;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCaseResultDTO {
        private int testIndex;
        private Boolean isHidden;
        private Boolean passed;
        private String input;
        private String expected;
        private String actual;
        private String error;
        private Long executionTimeMs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProblemSubmissionResponse {
        private Boolean isPassed;
        private Integer pointsAwarded;
        private Integer totalPoints;
        private Integer totalTestCases;
        private Integer passedTestCases;
        private Integer hiddenTestCasesCount;
        private Integer hiddenTestCasesPassed;
        private Long totalExecutionTimeMs;
        private String message;
        @Builder.Default
        private List<TestCaseResultDTO> results = new ArrayList<>();
    }
}
