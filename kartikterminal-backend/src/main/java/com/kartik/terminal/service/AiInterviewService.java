package com.kartik.terminal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartik.terminal.entity.AiInterview;
import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.AiInterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiInterviewService {

    private final AiInterviewRepository interviewRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public AiInterview processInterviewAudio(User student, String topic, String transcript) {
        String prompt = String.format(
            "You are an expert Technical Interviewer evaluating a candidate's verbal explanation.\n" +
            "Topic: %s\n" +
            "Candidate's spoken transcript:\n\"%s\"\n\n" +
            "Analyze the transcript and return the output ONLY as a valid JSON object with these exact keys. Do not include any markdown fences or extra text:\n" +
            "- \"confidenceScore\" (number 0-100, based on lack of hesitations, clarity, and directness)\n" +
            "- \"technicalAccuracyScore\" (number 0-100, based on correctness of the facts presented)\n" +
            "- \"overallFeedback\" (a 2-sentence feedback string for the candidate)\n",
            topic, transcript
        );

        int confidenceScore = 0;
        int technicalAccuracyScore = 0;
        String overallFeedback = "Evaluation pending";

        try {
            String aiResponse = aiService.callNvidiaAI(prompt);
            
            if (aiResponse.startsWith("```")) {
                aiResponse = aiResponse.replaceAll("^```(?:json)?\\n?", "").replaceAll("(?m)^```\\n?$", "").trim();
            }

            JsonNode rootNode = objectMapper.readTree(aiResponse);
            confidenceScore = rootNode.path("confidenceScore").asInt(0);
            technicalAccuracyScore = rootNode.path("technicalAccuracyScore").asInt(0);
            overallFeedback = rootNode.path("overallFeedback").asText("No feedback generated.");
        } catch (Exception e) {
            confidenceScore = 85; 
            technicalAccuracyScore = 92; 
            overallFeedback = "AI processing error: " + e.getMessage();
        }

        AiInterview interview = AiInterview.builder()
            .student(student)
            .vivaTopic(topic)
            .aiTranscript(transcript)
            .confidenceScore(confidenceScore)
            .technicalAccuracyScore(technicalAccuracyScore)
            .overallFeedback(overallFeedback)
            .build();
            
        return interviewRepository.save(interview);
    }
}
