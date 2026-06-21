package com.kartik.terminal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartik.terminal.entity.AiAnalysisReport;
import com.kartik.terminal.entity.ExecutionRecord;
import com.kartik.terminal.repository.AiAnalysisReportRepository;
import com.kartik.terminal.repository.ExecutionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {
    
    private final AiAnalysisReportRepository reportRepository;
    private final ExecutionRecordRepository executionRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public AiAnalysisReport analyzeCodeSubmission(Long executionRecordId) {
        ExecutionRecord record = executionRepository.findById(executionRecordId)
            .orElseThrow(() -> new RuntimeException("Execution not found"));

        String sourceCode = record.getCode() != null ? record.getCode() : "";
        if (sourceCode.isEmpty()) {
            throw new RuntimeException("No source code provided for analysis");
        }

        String prompt = String.format(
            "Analyze the following code for syntax, logic, security, and complexity.\n" +
            "Code:\n%s\n\n" +
            "You MUST return the output exclusively as a valid JSON object. Do NOT wrap in markdown.\n" +
            "JSON Keys:\n" +
            "- \"syntaxScore\" (integer 0-100)\n" +
            "- \"logicalScore\" (integer 0-100)\n" +
            "- \"securityVulnerabilities\" (string, comma separated list or 'None')\n" +
            "- \"timeComplexity\" (string, e.g., 'O(N)')\n" +
            "- \"spaceComplexity\" (string, e.g., 'O(1)')\n" +
            "- \"refactorSuggestions\" (string, JSON array of strings converted to string format)\n" +
            "- \"overallAiScore\" (integer 0-100)\n",
            sourceCode
        );

        AiAnalysisReport report;
        
        try {
            String aiResponse = aiService.callNvidiaAI(prompt);
            
            // Clean up possible markdown wrappers if the AI misbehaves
            if (aiResponse.startsWith("```json")) {
                aiResponse = aiResponse.substring(7);
            } else if (aiResponse.startsWith("```")) {
                aiResponse = aiResponse.substring(3);
            }
            if (aiResponse.endsWith("```")) {
                aiResponse = aiResponse.substring(0, aiResponse.length() - 3);
            }
            aiResponse = aiResponse.trim();
            
            JsonNode rootNode = objectMapper.readTree(aiResponse);
            
            report = AiAnalysisReport.builder()
                .executionRecord(record)
                .syntaxScore(rootNode.path("syntaxScore").asInt(0))
                .logicalScore(rootNode.path("logicalScore").asInt(0))
                .securityVulnerabilities(rootNode.path("securityVulnerabilities").asText("None"))
                .timeComplexity(rootNode.path("timeComplexity").asText("Unknown"))
                .spaceComplexity(rootNode.path("spaceComplexity").asText("Unknown"))
                .refactorSuggestions(rootNode.path("refactorSuggestions").asText("[]"))
                .overallAiScore(rootNode.path("overallAiScore").asInt(0))
                .build();
                
        } catch (Exception e) {
            log.error("AI Analysis failed for execution " + executionRecordId, e);
            // Fallback object on failure
            report = AiAnalysisReport.builder()
                .executionRecord(record)
                .syntaxScore(0)
                .logicalScore(0)
                .securityVulnerabilities("[\"Analysis Failed\"]")
                .timeComplexity("N/A")
                .spaceComplexity("N/A")
                .refactorSuggestions("{\"error\": \"AI service unavailable\"}")
                .overallAiScore(0)
                .build();
        }
            
        return reportRepository.save(report);
    }
}
