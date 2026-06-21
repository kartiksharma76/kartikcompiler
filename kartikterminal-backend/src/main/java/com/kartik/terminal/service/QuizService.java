package com.kartik.terminal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartik.terminal.dto.QuizDTOs.*;
import com.kartik.terminal.entity.*;
import com.kartik.terminal.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    // === Admin / Teacher Functions ===

    @Transactional
    public Quiz createQuiz(QuizRequest request) {
        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .topic(request.getTopic())
                .difficulty(request.getDifficulty())
                .isAiGenerated(request.isAiGenerated())
                .build();
        return quizRepository.save(quiz);
    }

    @Transactional
    public Question addQuestion(Long quizId, QuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));

        Question question = Question.builder()
                .quiz(quiz)
                .text(request.getText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctAnswer(request.getCorrectAnswer())
                .aiExplanation(request.getAiExplanation())
                .build();
        return questionRepository.save(question);
    }

    @Transactional
    public Quiz generateQuizWithAI(AIQuizGenerationRequest request) {
        // Call AI to get questions FIRST, before writing to MySQL
        String jsonStr = aiService.generateQuizQuestionsJson(request.getTopic(), request.getDifficulty(), request.getCount());
        
        // Robust extraction of JSON array
        try {
            int startIndex = jsonStr.indexOf('[');
            int endIndex = jsonStr.lastIndexOf(']');
            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                jsonStr = jsonStr.substring(startIndex, endIndex + 1);
            } else {
                throw new RuntimeException("Could not find JSON array bounds in AI output.");
            }
            
            // Only parse if we found valid bounds
            List<QuestionRequest> parsedQuestions = objectMapper.readValue(jsonStr, new com.fasterxml.jackson.core.type.TypeReference<List<QuestionRequest>>() {});
            
            // If parsed correctly, NOW we commit the Quiz to the Database
            Quiz quiz = Quiz.builder()
                    .title("AI " + request.getDifficulty() + " " + request.getTopic() + " Quiz")
                    .description("Dynamically generated quiz by Artificial Intelligence.")
                    .topic(request.getTopic())
                    .difficulty(request.getDifficulty())
                    .isAiGenerated(true)
                    .build();
            quiz = quizRepository.save(quiz);

            // Add all freshly verified questions to the Quiz
            for (QuestionRequest qr : parsedQuestions) {
                addQuestion(quiz.getId(), qr);
            }
            
            return quiz;
            
        } catch (Exception e) {
            log.error("Failed to parse AI generated questions or generation crashed: {}", jsonStr, e);
            throw new RuntimeException("Failed to generate questions. Ensure NVIDIA API is active and functioning.");
        }
    }


    // === User Functions ===

    @Transactional(readOnly = true)
    public List<Quiz> getAvailableQuizzes() {
        return quizRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Question> getQuizQuestions(Long quizId) {
        return questionRepository.findByQuizId(quizId);
    }

    @Transactional
    public QuizSubmission submitQuiz(QuizSubmissionRequest request) {
        User user = authService.getCurrentUser();
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        
        int score = 0;
        for (AnswerRequest ansReq : request.getAnswers()) {
            Question q = questions.stream()
                .filter(question -> question.getId().equals(ansReq.getQuestionId()))
                .findFirst().orElse(null);
            
            if (q != null && q.getCorrectAnswer().equalsIgnoreCase(ansReq.getSelectedOption())) {
                score++;
            }
        }

        QuizSubmission doc = QuizSubmission.builder()
                .user(user)
                .quiz(quiz)
                .score(score)
                .totalQuestions(questions.size())
                .build();
        
        submissionRepository.save(doc);

        // Update User Quiz Points (e.g. 10 points per correct answer)
        user.setQuizPoints(user.getQuizPoints() + (score * 10));
        userRepository.save(user);

        return doc;
    }

    // AI Solving Endpoint for a single question
    public String getAiHint(Long questionId) {
        Question q = questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("Question not found"));
            
        return aiService.solveQuizQuestion(q.getText(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD());
    }

    @Transactional(readOnly = true)
    public String getQuizAiReview(Long submissionId) {
        QuizSubmission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Submission not found"));
        
        Quiz quiz = submission.getQuiz();
        List<Question> questions = questionRepository.findByQuizId(quiz.getId());
        
        StringBuilder questionsText = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            questionsText.append(String.format("Question %d: %s\n- Option A: %s\n- Option B: %s\n- Option C: %s\n- Option D: %s\n- Correct Answer: %s\n- Explanation: %s\n\n",
                i + 1, q.getText(), q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD(), q.getCorrectAnswer(), q.getAiExplanation()));
        }

        String prompt = String.format(
            "You are an expert technical interviewer and educator.\n" +
            "Analyze this user's quiz performance and provide a professional, constructive, and detailed conceptual feedback report in clean Markdown.\n\n" +
            "Quiz Details:\n" +
            "- Title: %s\n" +
            "- Topic: %s\n" +
            "- Difficulty: %s\n" +
            "- User Score: %d out of %d correct answers\n\n" +
            "Questions Covered in this Quiz:\n%s" +
            "Please format your response using standard GitHub-style Markdown. Include sections:\n" +
            "1. **Overall Performance Summary** (Encouraging tone, analyzing their score/percentage)\n" +
            "2. **Conceptual Strengths** (Identify what they likely understood based on the quiz topic)\n" +
            "3. **Suggested Areas of Improvement** (Provide specific sub-topics and conceptual pointers)\n" +
            "4. **Personalized Action Plan** (3 concrete steps they can take next, e.g., coding exercise, reading documentation)",
            quiz.getTitle(),
            quiz.getTopic(),
            quiz.getDifficulty(),
            submission.getScore(),
            submission.getTotalQuestions(),
            questionsText.toString()
        );

        return aiService.callNvidiaAI(prompt);
    }
}
