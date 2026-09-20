package com.kartik.terminal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kartik.terminal.dto.ProblemDTOs.*;
import com.kartik.terminal.entity.Institution;
import com.kartik.terminal.entity.Problem;
import com.kartik.terminal.entity.ProblemSubmission;
import com.kartik.terminal.entity.TestCase;
import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.InstitutionRepository;
import com.kartik.terminal.repository.ProblemRepository;
import com.kartik.terminal.repository.ProblemSubmissionRepository;
import com.kartik.terminal.repository.TestCaseRepository;
import com.kartik.terminal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProblemSubmissionRepository problemSubmissionRepository;
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final CompilerService compilerService;
    private final AIService aiService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    // ──────────────────────────────────────────────────────────
    // 1. Assign/Create Problem (Platform Admin or College Admin)
    // ──────────────────────────────────────────────────────────
    @Transactional
    public ProblemResponse assignProblem(ProblemRequest request, User currentUser) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("Problem title is required.");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("Problem description is required.");
        }

        // Determine Institution Scoping
        Institution assignedInstitution = null;
        boolean isPlatformAdmin = currentUser.getRole() == User.Role.ADMIN || currentUser.getRole() == User.Role.SUPER_ADMIN;

        if (isPlatformAdmin) {
            // Platform admin can explicitly assign to a college or leave null for global (visible to ALL students)
            if (Boolean.FALSE.equals(request.getAssignGlobally()) && request.getTargetInstitutionId() != null) {
                assignedInstitution = institutionRepository.findById(request.getTargetInstitutionId()).orElse(null);
            }
        } else if (currentUser.getRole() == User.Role.COLLEGE_ADMIN || currentUser.getRole() == User.Role.FACULTY) {
            // College Admin / Faculty assignments are always scoped to their college
            assignedInstitution = currentUser.getInstitution();
        }

        // Build Problem
        Problem problem = Problem.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .difficulty(request.getDifficulty() != null ? request.getDifficulty().toUpperCase() : "MEDIUM")
                .points(request.getPoints() != null && request.getPoints() > 0 ? request.getPoints() : 50)
                .tags(request.getTags() != null ? request.getTags().trim() : "Algorithms")
                .sampleInput(request.getSampleInput() != null ? request.getSampleInput() : "")
                .sampleOutput(request.getSampleOutput() != null ? request.getSampleOutput() : "")
                .starterCode(request.getStarterCode() != null ? request.getStarterCode() : "")
                .institution(assignedInstitution)
                .createdBy(currentUser)
                .build();

        // Process Test Cases (1 public sample + 2 hidden test cases)
        List<TestCase> testCases = new ArrayList<>();
        if (request.getTestCases() != null && !request.getTestCases().isEmpty()) {
            for (TestCaseDTO tcDTO : request.getTestCases()) {
                String in = tcDTO.getInputData() != null ? tcDTO.getInputData().trim() : "";
                String out = tcDTO.getExpectedOutput() != null ? tcDTO.getExpectedOutput().trim() : "";
                if (!in.isEmpty() || !out.isEmpty()) {
                    testCases.add(TestCase.builder()
                            .problem(problem)
                            .inputData(in)
                            .expectedOutput(out)
                            .isHidden(Boolean.TRUE.equals(tcDTO.getIsHidden()))
                            .build());
                }
            }
        }

        // If no test cases were provided, seed 1 public sample and 2 hidden test cases
        if (testCases.isEmpty()) {
            String sIn = request.getSampleInput() != null && !request.getSampleInput().isBlank() ? request.getSampleInput().trim() : "5\n1 2 3 4 5";
            String sOut = request.getSampleOutput() != null && !request.getSampleOutput().isBlank() ? request.getSampleOutput().trim() : "15";
            testCases.add(TestCase.builder().problem(problem).inputData(sIn).expectedOutput(sOut).isHidden(false).build());
            testCases.add(TestCase.builder().problem(problem).inputData("3\n10 20 30").expectedOutput("60").isHidden(true).build());
            testCases.add(TestCase.builder().problem(problem).inputData("4\n-5 5 -10 10").expectedOutput("0").isHidden(true).build());
        }
        problem.setTestCases(testCases);

        Problem saved = problemRepository.save(problem);
        if (testCases != null && !testCases.isEmpty()) {
            testCaseRepository.saveAll(testCases);
        }
        log.info("Problem assigned: ID={}, Title='{}', Scope={}, CreatedBy={}, TotalTests={}",
                saved.getId(), saved.getTitle(), assignedInstitution != null ? assignedInstitution.getName() : "GLOBAL", currentUser.getUsername(), testCases.size());

        return mapToResponse(saved, currentUser);
    }

    // ──────────────────────────────────────────────────────────
    // 2. AI Problem & 2 Hidden Test Cases Generator
    // ──────────────────────────────────────────────────────────
    public Map<String, Object> generateAiProblem(AiGenerateProblemRequest req) {
        String topic = req.getTopic() != null && !req.getTopic().isBlank() ? req.getTopic() : "Array & Hashing Optimization";
        String difficulty = req.getDifficulty() != null ? req.getDifficulty().toUpperCase() : "MEDIUM";
        String language = req.getLanguage() != null ? req.getLanguage() : "Java";

        String prompt = String.format(
            "You are an expert Competitive Programming and Coding Assessment problem creator.\n" +
            "Generate a complete coding problem statement about topic: '%s' with difficulty: '%s' for target language: '%s'.\n" +
            "You MUST return ONLY a valid JSON object without markdown fences, with these EXACT keys:\n" +
            "{\n" +
            "  \"title\": \"Title of problem\",\n" +
            "  \"description\": \"Detailed problem description with constraints, input/output formats, and explanation\",\n" +
            "  \"difficulty\": \"%s\",\n" +
            "  \"points\": 50,\n" +
            "  \"tags\": \"Algorithms, Data Structures\",\n" +
            "  \"sampleInput\": \"Sample input provided to student\",\n" +
            "  \"sampleOutput\": \"Sample expected output\",\n" +
            "  \"starterCode\": \"Starter boilerplate code template in %s with class Solution or Main\",\n" +
            "  \"testCases\": [\n" +
            "    {\"inputData\": \"Sample input 1\", \"expectedOutput\": \"Sample output 1\", \"isHidden\": false},\n" +
            "    {\"inputData\": \"Hidden edge case input 1\", \"expectedOutput\": \"Expected output 1\", \"isHidden\": true},\n" +
            "    {\"inputData\": \"Hidden edge case input 2\", \"expectedOutput\": \"Expected output 2\", \"isHidden\": true}\n" +
            "  ]\n" +
            "}\n" +
            "CRITICAL: Ensure exactly 1 public sample test case (isHidden: false) and 2 robust hidden test cases (isHidden: true) covering edge cases and boundary conditions.",
            topic, difficulty, language, difficulty, language
        );

        String aiResponse = aiService.callNvidiaAI(prompt);
        return parseAiProblemResponse(aiResponse, topic, difficulty, language);
    }

    private Map<String, Object> parseAiProblemResponse(String raw, String topic, String diff, String lang) {
        try {
            String json = cleanJson(raw);
            JsonNode root = objectMapper.readTree(json);
            if (root.isObject() && root.has("title") && root.has("description")) {
                Map<String, Object> map = objectMapper.convertValue(root, Map.class);
                if (map.get("testCases") != null) {
                    return map;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI JSON response for problem generation (raw length={}), using diverse dynamic generator: {}", 
                raw != null ? raw.length() : 0, e.getMessage());
        }

        // Diverse dynamic problem generator covering multiple algorithms to prevent repetition
        long seed = System.currentTimeMillis() % 6;
        String title;
        String desc;
        String sampleIn;
        String sampleOut;
        List<Map<String, Object>> tcs;

        if (seed == 0 || topic.toLowerCase().contains("array") || topic.toLowerCase().contains("sum")) {
            title = "Two Sum Target Finder";
            desc = "### Problem Statement\nGiven an array of integers `nums` and an integer `target`, find whether two distinct numbers add up to `target`. Print `YES` if such a pair exists, otherwise print `NO`.\n\n### Constraints\n- 2 <= N <= 10^5\n- -10^9 <= nums[i], target <= 10^9\n\n### Input Format\n- First line: integer `N` (number of elements) and integer `target`\n- Second line: `N` space-separated integers\n\n### Output Format\nPrint `YES` or `NO`.";
            sampleIn = "4 9\n2 7 11 15";
            sampleOut = "YES";
            tcs = List.of(
                Map.of("inputData", "4 9\n2 7 11 15", "expectedOutput", "YES", "isHidden", false),
                Map.of("inputData", "3 6\n3 2 4", "expectedOutput", "YES", "isHidden", true),
                Map.of("inputData", "4 20\n1 2 3 4", "expectedOutput", "NO", "isHidden", true)
            );
        } else if (seed == 1 || topic.toLowerCase().contains("string") || topic.toLowerCase().contains("palindrome")) {
            title = "Palindrome String Verifier";
            desc = "### Problem Statement\nGiven a string `S`, determine if it is a palindrome considering only alphanumeric characters and ignoring cases. Print `true` if it is a palindrome, else `false`.\n\n### Constraints\n- 1 <= |S| <= 10^5\n\n### Input Format\nSingle line containing string `S`.\n\n### Output Format\nPrint `true` or `false`.";
            sampleIn = "racecar";
            sampleOut = "true";
            tcs = List.of(
                Map.of("inputData", "racecar", "expectedOutput", "true", "isHidden", false),
                Map.of("inputData", "hello", "expectedOutput", "false", "isHidden", true),
                Map.of("inputData", "madam", "expectedOutput", "true", "isHidden", true)
            );
        } else if (seed == 2 || topic.toLowerCase().contains("max") || topic.toLowerCase().contains("subarray")) {
            title = "Maximum Subarray Sum (Kadane)";
            desc = "### Problem Statement\nGiven an integer array `nums`, find the contiguous subarray which has the largest sum and return its sum.\n\n### Constraints\n- 1 <= N <= 10^5\n- -10^4 <= nums[i] <= 10^4\n\n### Input Format\nFirst line contains integer `N`. Second line contains `N` integers.\n\n### Output Format\nPrint the maximum subarray sum.";
            sampleIn = "5\n-2 1 -3 4 -1";
            sampleOut = "4";
            tcs = List.of(
                Map.of("inputData", "5\n-2 1 -3 4 -1", "expectedOutput", "4", "isHidden", false),
                Map.of("inputData", "4\n1 2 3 4", "expectedOutput", "10", "isHidden", true),
                Map.of("inputData", "3\n-5 -2 -3", "expectedOutput", "-2", "isHidden", true)
            );
        } else if (seed == 3 || topic.toLowerCase().contains("search") || topic.toLowerCase().contains("binary")) {
            title = "Binary Search Index Locator";
            desc = "### Problem Statement\nGiven a sorted array of `N` distinct integers and a target value `K`, return the 0-based index if the target is found. If not, return `-1`.\n\n### Constraints\n- 1 <= N <= 10^5\n- -10^9 <= nums[i], K <= 10^9\n\n### Input Format\nFirst line: `N` and `K`\nSecond line: `N` sorted integers\n\n### Output Format\nPrint target index or -1.";
            sampleIn = "5 9\n-1 0 3 5 9";
            sampleOut = "4";
            tcs = List.of(
                Map.of("inputData", "5 9\n-1 0 3 5 9", "expectedOutput", "4", "isHidden", false),
                Map.of("inputData", "4 2\n1 3 5 6", "expectedOutput", "-1", "isHidden", true),
                Map.of("inputData", "1 10\n10", "expectedOutput", "0", "isHidden", true)
            );
        } else if (seed == 4 || topic.toLowerCase().contains("hash") || topic.toLowerCase().contains("duplicate")) {
            title = "Find Duplicate Number in Array";
            desc = "### Problem Statement\nGiven an array containing `N` numbers, determine if any value appears at least twice in the array. Print `true` if any value appears at least twice, and `false` if every element is distinct.\n\n### Constraints\n- 1 <= N <= 10^5\n\n### Input Format\nFirst line contains integer `N`, second line contains `N` integers.\n\n### Output Format\nPrint `true` or `false`.";
            sampleIn = "4\n1 2 3 1";
            sampleOut = "true";
            tcs = List.of(
                Map.of("inputData", "4\n1 2 3 1", "expectedOutput", "true", "isHidden", false),
                Map.of("inputData", "4\n1 2 3 4", "expectedOutput", "false", "isHidden", true),
                Map.of("inputData", "3\n5 5 5", "expectedOutput", "true", "isHidden", true)
            );
        } else {
            title = "Reverse Array In-Place";
            desc = "### Problem Statement\nGiven an array of `N` integers, print the elements in reversed order separated by spaces.\n\n### Constraints\n- 1 <= N <= 10^5\n\n### Input Format\nFirst line: integer `N`\nSecond line: `N` integers\n\n### Output Format\nPrint reversed integers on a single line.";
            sampleIn = "4\n1 2 3 4";
            sampleOut = "4 3 2 1";
            tcs = List.of(
                Map.of("inputData", "4\n1 2 3 4", "expectedOutput", "4 3 2 1", "isHidden", false),
                Map.of("inputData", "3\n10 20 30", "expectedOutput", "30 20 10", "isHidden", true),
                Map.of("inputData", "1\n99", "expectedOutput", "99", "isHidden", true)
            );
        }

        String starter = "java".equalsIgnoreCase(lang) ?
                "import java.util.*;\nimport java.io.*;\n\npublic class Solution {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        if (sc.hasNextInt()) {\n            int n = sc.nextInt();\n            // Complete your solution logic below:\n            System.out.println(n);\n        }\n    }\n}" :
                "# Write your optimal solution here\nimport sys\n\ndef solve():\n    lines = sys.stdin.read().split()\n    if not lines: return\n    n = int(lines[0])\n    print(n)\n\nif __name__ == '__main__':\n    solve()\n";

        return Map.of(
            "title", title,
            "description", desc,
            "difficulty", diff,
            "points", 50,
            "tags", topic + ", DSA",
            "sampleInput", sampleIn,
            "sampleOutput", sampleOut,
            "starterCode", starter,
            "testCases", tcs
        );
    }

    private String cleanJson(String raw) {
        if (raw == null || raw.isBlank()) return "{}";
        int firstBrace = raw.indexOf('{');
        int lastBrace = raw.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return raw.substring(firstBrace, lastBrace + 1).trim();
        }
        return raw.trim();
    }

    // ──────────────────────────────────────────────────────────
    // 3. Get Problems for User (Scoped to Global + User's College)
    // ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ProblemResponse> getProblemsForUser(User user) {
        List<Problem> problems;

        if (user == null) {
            // Public/Anonymous view: only global problems
            problems = problemRepository.findByInstitutionIsNullOrderByIdDesc();
        } else if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN) {
            // Platform Admins see ALL problems
            problems = problemRepository.findAllByOrderByIdDesc();
        } else if (user.getInstitution() != null) {
            // College student / College admin sees Global Problems + Problems for their institution
            problems = problemRepository.findByInstitutionOrInstitutionIsNullOrderByIdDesc(user.getInstitution());
        } else {
            // Normal non-college student sees Global Problems
            problems = problemRepository.findByInstitutionIsNullOrderByIdDesc();
        }

        return problems.stream()
                .map(p -> mapToResponse(p, user))
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────
    // 4. Get Single Problem (with public test cases only)
    // ──────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProblemResponse getProblemById(Long id, User user) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Problem not found with ID: " + id));

        // Check access if problem is restricted to a college
        if (problem.getInstitution() != null && user != null) {
            boolean isPlatformAdmin = user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.SUPER_ADMIN;
            boolean isSameCollege = user.getInstitution() != null && user.getInstitution().getId().equals(problem.getInstitution().getId());
            if (!isPlatformAdmin && !isSameCollege) {
                throw new SecurityException("This problem assignment is restricted to students of " + problem.getInstitution().getName());
            }
        }

        return mapToResponse(problem, user);
    }

    // ──────────────────────────────────────────────────────────
    // 5. Submit Problem Solution & Grade against 2 Hidden Test Cases
    // ──────────────────────────────────────────────────────────
    @Transactional
    public ProblemSubmissionResponse submitSolution(Long problemId, ProblemSubmissionRequest request, User user) {
        if (user.getIsDisqualified() != null && user.getIsDisqualified()) {
            throw new IllegalStateException("Your account is disqualified due to anti-cheat violations.");
        }
        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new IllegalStateException("Account is deactivated.");
        }

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("Problem not found with ID: " + problemId));

        List<TestCase> testCases = testCaseRepository.findByProblemOrderByIdAsc(problem);
        if (testCases == null || testCases.isEmpty()) {
            testCases = problem.getTestCases();
        }

        // If no test cases exist, auto-create 1 sample + 2 hidden test cases
        if (testCases == null || testCases.isEmpty()) {
            testCases = new ArrayList<>();
            String sIn = problem.getSampleInput() != null && !problem.getSampleInput().isBlank() ? problem.getSampleInput().trim() : "5\n1 2 3 4 5";
            String sOut = problem.getSampleOutput() != null && !problem.getSampleOutput().isBlank() ? problem.getSampleOutput().trim() : "15";
            testCases.add(TestCase.builder().problem(problem).inputData(sIn).expectedOutput(sOut).isHidden(false).build());
            testCases.add(TestCase.builder().problem(problem).inputData("3\n10 20 30").expectedOutput("60").isHidden(true).build());
            testCases.add(TestCase.builder().problem(problem).inputData("4\n-5 5 -10 10").expectedOutput("0").isHidden(true).build());
            testCaseRepository.saveAll(testCases);
        }

        int totalCases = testCases.size();
        int passedCount = 0;
        int hiddenTotal = 0;
        int hiddenPassed = 0;
        long totalExecTime = 0L;
        List<TestCaseResultDTO> results = new ArrayList<>();

        int testIndex = 1;
        for (TestCase tc : testCases) {
            boolean isHidden = Boolean.TRUE.equals(tc.getIsHidden());
            if (isHidden) hiddenTotal++;

            CompilerService.RawExecutionResult execResult = compilerService.runRawSandbox(
                    request.getLanguage(),
                    request.getCode(),
                    tc.getInputData() != null ? tc.getInputData() : ""
            );

            totalExecTime += execResult.executionTimeMs();

            String expectedClean = tc.getExpectedOutput() != null ? tc.getExpectedOutput().trim().replace("\r\n", "\n") : "";
            String actualClean = execResult.output() != null ? execResult.output().trim().replace("\r\n", "\n") : "";

            // Normalize whitespace between lines and trim
            expectedClean = expectedClean.lines().map(String::stripTrailing).collect(Collectors.joining("\n")).trim();
            actualClean = actualClean.lines().map(String::stripTrailing).collect(Collectors.joining("\n")).trim();

            // Strict validation: Must exit code 0, have NO error, have non-empty expected, and match exactly
            boolean passed = execResult.exitCode() == 0 &&
                             (execResult.error() == null || execResult.error().trim().isEmpty()) &&
                             !expectedClean.isEmpty() &&
                             expectedClean.equals(actualClean);

            if (passed) {
                passedCount++;
                if (isHidden) hiddenPassed++;
            }

            // Prepare student view of result
            TestCaseResultDTO r = TestCaseResultDTO.builder()
                    .testIndex(testIndex++)
                    .isHidden(isHidden)
                    .passed(passed)
                    .executionTimeMs(execResult.executionTimeMs())
                    .error(execResult.error())
                    .input(isHidden ? "[Hidden Test Case]" : tc.getInputData())
                    .expected(isHidden ? "[Hidden Expected Output]" : tc.getExpectedOutput())
                    .actual(isHidden ? (passed ? "[Matched Expected Output]" : "[Output Mismatch]") : execResult.output())
                    .build();

            results.add(r);
        }

        // Verdict: Passed ONLY if ALL test cases pass
        boolean isAllPassed = (totalCases > 0) && (passedCount == totalCases);

        int pointsToAward = 0;
        boolean alreadySolved = problemSubmissionRepository.existsByUserAndProblemAndIsSolvedTrue(user, problem);

        if (isAllPassed) {
            if (!alreadySolved) {
                pointsToAward = problem.getPoints() != null ? problem.getPoints() : 50;
                // Add points to student account
                user.setTotalPoints((user.getTotalPoints() != null ? user.getTotalPoints() : 0) + pointsToAward);
                user.setSuccessfulExecutions((user.getSuccessfulExecutions() != null ? user.getSuccessfulExecutions() : 0) + 1);
            }
        }
        user.setTotalExecutions((user.getTotalExecutions() != null ? user.getTotalExecutions() : 0) + 1);
        userRepository.save(user);

        // Record Submission
        ProblemSubmission submission = ProblemSubmission.builder()
                .user(user)
                .problem(problem)
                .language(request.getLanguage())
                .code(request.getCode())
                .isSolved(isAllPassed)
                .testCasesPassed(passedCount)
                .totalTestCases(totalCases)
                .executionTimeMs(totalExecTime)
                .build();
        problemSubmissionRepository.save(submission);

        String message;
        if (isAllPassed) {
            message = String.format("🎉 ACCEPTED! All %d test cases (including %d hidden test cases) passed flawlessly! +%d Points credited.",
                    totalCases, hiddenTotal, pointsToAward);
        } else {
            message = String.format("❌ WRONG ANSWER: %d/%d test cases passed (%d/%d hidden test cases passed). Points will only be awarded when all 4 hidden test cases pass.",
                    passedCount, totalCases, hiddenPassed, hiddenTotal);
        }

        return ProblemSubmissionResponse.builder()
                .isPassed(isAllPassed)
                .pointsAwarded(pointsToAward)
                .totalPoints(user.getTotalPoints())
                .totalTestCases(totalCases)
                .passedTestCases(passedCount)
                .hiddenTestCasesCount(hiddenTotal)
                .hiddenTestCasesPassed(hiddenPassed)
                .totalExecutionTimeMs(totalExecTime)
                .message(message)
                .results(results)
                .build();
    }

    // ──────────────────────────────────────────────────────────
    // 6. Delete Problem
    // ──────────────────────────────────────────────────────────
    @Transactional
    public void deleteProblem(Long id, User currentUser) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Problem not found with ID: " + id));

        boolean isPlatformAdmin = currentUser.getRole() == User.Role.ADMIN || currentUser.getRole() == User.Role.SUPER_ADMIN;
        boolean isCollegeOwner = (currentUser.getRole() == User.Role.COLLEGE_ADMIN || currentUser.getRole() == User.Role.FACULTY) &&
                problem.getInstitution() != null && currentUser.getInstitution() != null &&
                problem.getInstitution().getId().equals(currentUser.getInstitution().getId());

        if (!isPlatformAdmin && !isCollegeOwner) {
            throw new SecurityException("Permission Denied: You do not have rights to delete this problem.");
        }

        problemSubmissionRepository.deleteByProblemId(id);
        testCaseRepository.deleteByProblemId(id);
        problemRepository.delete(problem);
        log.info("Problem deleted ID={}, Title='{}' by user={}", id, problem.getTitle(), currentUser.getUsername());
    }

    // ──────────────────────────────────────────────────────────
    // Helper Mapper
    // ──────────────────────────────────────────────────────────
    private ProblemResponse mapToResponse(Problem p, User currentUser) {
        List<TestCase> testCases = p.getTestCases() != null ? p.getTestCases() : List.of();
        int total = testCases.size();
        int hidden = (int) testCases.stream().filter(tc -> Boolean.TRUE.equals(tc.getIsHidden())).count();

        // Only expose non-hidden test cases to client
        List<TestCaseDTO> publicTests = testCases.stream()
                .filter(tc -> !Boolean.TRUE.equals(tc.getIsHidden()))
                .map(tc -> TestCaseDTO.builder()
                        .id(tc.getId())
                        .inputData(tc.getInputData())
                        .expectedOutput(tc.getExpectedOutput())
                        .isHidden(false)
                        .build())
                .collect(Collectors.toList());

        boolean isSolved = false;
        if (currentUser != null) {
            isSolved = problemSubmissionRepository.existsByUserAndProblemAndIsSolvedTrue(currentUser, p);
        }

        return ProblemResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .difficulty(p.getDifficulty())
                .points(p.getPoints())
                .tags(p.getTags())
                .sampleInput(p.getSampleInput())
                .sampleOutput(p.getSampleOutput())
                .starterCode(p.getStarterCode())
                .isGlobal(p.getInstitution() == null)
                .institutionId(p.getInstitution() != null ? p.getInstitution().getId() : null)
                .institutionName(p.getInstitution() != null ? p.getInstitution().getName() : "Global Platform Assignment")
                .createdByUsername(p.getCreatedBy() != null ? p.getCreatedBy().getUsername() : "Admin")
                .createdAt(p.getCreatedAt())
                .isSolved(isSolved)
                .totalTestCasesCount(total)
                .hiddenTestCasesCount(hidden)
                .publicTestCases(publicTests)
                .build();
    }
}
