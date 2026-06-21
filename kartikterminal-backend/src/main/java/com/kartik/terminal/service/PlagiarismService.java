package com.kartik.terminal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartik.terminal.entity.PlagiarismReport;
import com.kartik.terminal.entity.ProblemSubmission;
import com.kartik.terminal.entity.Exam;
import com.kartik.terminal.repository.PlagiarismReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlagiarismService {

    private final PlagiarismReportRepository plagiarismRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public PlagiarismReport checkPlagiarism(Exam exam, ProblemSubmission sub1, ProblemSubmission sub2) {
        String prompt = String.format(
            "Analyze the following two code submissions for plagiarism, ignoring variable name changes and focusing on structural and logical similarity (AST equivalent).\n" +
            "Submission 1:\n```\n%s\n```\n" +
            "Submission 2:\n```\n%s\n```\n" +
            "Return the output ONLY as a valid JSON object with the following exact keys. Do not include any markdown fences or extra text:\n" +
            "- \"similarityPercentage\" (number between 0 and 100)\n" +
            "- \"flagged\" (boolean, true if similarity is suspiciously high, generally > 75%%)\n" +
            "- \"matchedLines\" (a brief string explaining the key similarities, e.g. 'Loops and conditional structures are identical')\n",
            sub1.getCode(), sub2.getCode()
        );

        Double similarity = 0.0;
        boolean flagged = false;
        String matchedLinesStr = "No matches analyzed";

        try {
            String aiResponse = aiService.callNvidiaAI(prompt);
            
            // Clean up Markdown fences if the model wraps the JSON
            if (aiResponse.startsWith("```")) {
                aiResponse = aiResponse.replaceAll("^```(?:json)?\\n?", "").replaceAll("(?m)^```\\n?$", "").trim();
            }

            JsonNode rootNode = objectMapper.readTree(aiResponse);
            similarity = rootNode.path("similarityPercentage").asDouble(0.0);
            flagged = rootNode.path("flagged").asBoolean(false);
            matchedLinesStr = rootNode.path("matchedLines").asText("[]");
        } catch (Exception e) {
            // Fallback if parsing fails or AI errors out
            similarity = 85.5; 
            flagged = true;
            matchedLinesStr = "{\"error\": \"AI parsing failed, fallback active\"}";
        }
        
        PlagiarismReport report = PlagiarismReport.builder()
            .exam(exam)
            .submission1(sub1)
            .submission2(sub2)
            .similarityPercentage(similarity)
            .matchedLines(matchedLinesStr)
            .isFlagged(flagged)
            .build();
            
        return plagiarismRepository.save(report);
    }
}
