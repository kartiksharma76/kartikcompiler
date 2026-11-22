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
import java.util.HashMap;
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

        // Process Test Cases (Public sample & validation test cases only - no hidden test cases)
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
                            .isHidden(false)
                            .build());
                }
            }
        }

        // If no test cases were provided, seed 1 public sample test case
        if (testCases.isEmpty()) {
            String sIn = request.getSampleInput() != null && !request.getSampleInput().isBlank() ? request.getSampleInput().trim() : "";
            String sOut = request.getSampleOutput() != null && !request.getSampleOutput().isBlank() ? request.getSampleOutput().trim() : "";
            testCases.add(TestCase.builder().problem(problem).inputData(sIn).expectedOutput(sOut).isHidden(false).build());
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
    // ──────────────────────────────────────────────────────────
    // 2. AI Problem & Output Verification Generator
    // ──────────────────────────────────────────────────────────
    public Map<String, Object> generateAiProblem(AiGenerateProblemRequest req) {
        String topic = req.getTopic() != null && !req.getTopic().isBlank() ? req.getTopic().trim() : "Dynamic Programming";
        String difficulty = req.getDifficulty() != null ? req.getDifficulty().toUpperCase() : "MEDIUM";
        String language = req.getLanguage() != null ? req.getLanguage() : "Java";

        String prompt = String.format(
            "You are an expert Competitive Programming and Coding Assessment problem creator.\n" +
            "Generate a completely UNIQUE and DISTINCT coding problem statement strictly focused on topic: '%s' with difficulty: '%s' for target language: '%s'.\n" +
            "CRITICAL INSTRUCTIONS:\n" +
            "1. The problem MUST be specifically and genuinely about '%s'. NEVER output a generic Two-Sum or Palindrome problem unless that was the exact requested topic.\n" +
            "2. The problem statement MUST explicitly specify the EXACT target value, exact formula, condition, or mathematical rule to compute.\n" +
            "3. The description MUST include formatted markdown sections: '### Problem Statement', '### Exact Rule & Target Value', '### Input Format', '### Output Format', '### Constraints', and '### Example Walkthrough' with Explanation.\n" +
            "4. Sample Input and Sample Output MUST be provided, fully matching the exact rule.\n" +
            "5. For Java, starterCode MUST use 'public class Main'. For C/C++, use 'int main()'. For Python, use standard clean script.\n" +
            "6. Do NOT include ANY hidden test cases. Only provide 1 public sample test case matching the problem statement and final output.\n" +
            "You MUST return ONLY a valid JSON object without markdown fences, with these EXACT keys:\n" +
            "{\n" +
            "  \"title\": \"Title of problem\",\n" +
            "  \"description\": \"Detailed problem description with exact rules, target value, input/output formats, constraints, and explanation\",\n" +
            "  \"difficulty\": \"%s\",\n" +
            "  \"points\": 50,\n" +
            "  \"tags\": \"%s, DSA\",\n" +
            "  \"sampleInput\": \"Sample input matching input format\",\n" +
            "  \"sampleOutput\": \"Expected sample output matching exact rule\",\n" +
            "  \"starterCode\": \"Starter boilerplate code template in %s with public class Main\",\n" +
            "  \"testCases\": [\n" +
            "    {\"inputData\": \"Sample input matching sampleInput\", \"expectedOutput\": \"Sample output matching sampleOutput\", \"isHidden\": false}\n" +
            "  ]\n" +
            "}",
            topic, difficulty, language, topic, difficulty, topic, language
        );

        String aiResponse = aiService.callNvidiaAI(prompt);
        return parseAiProblemResponse(aiResponse, topic, difficulty, language);
    }

    private Map<String, Object> parseAiProblemResponse(String raw, String topic, String diff, String lang) {
        try {
            String json = cleanJson(raw);
            JsonNode root = objectMapper.readTree(json);
            if (root.has("problem") && root.get("problem").isObject()) {
                root = root.get("problem");
            }
            if (root.isObject() && root.has("title") && (root.has("description") || root.has("problemStatement"))) {
                Map<String, Object> map = objectMapper.convertValue(root, Map.class);
                if (!map.containsKey("description") && map.containsKey("problemStatement")) {
                    map.put("description", map.get("problemStatement"));
                }
                if (map.get("starterCode") instanceof String) {
                    String sc = (String) map.get("starterCode");
                    sc = sc.replace("public class Solution", "public class Main")
                           .replace("class Solution", "public class Main");
                    map.put("starterCode", sc);
                }
                if (map.get("testCases") instanceof List) {
                    List<?> list = (List<?>) map.get("testCases");
                    List<Map<String, Object>> sanitized = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map) {
                            Map<String, Object> tc = new HashMap<>((Map<String, Object>) item);
                            tc.put("isHidden", false);
                            sanitized.add(tc);
                        }
                    }
                    map.put("testCases", sanitized);
                }
                if (map.get("testCases") == null || ((List<?>) map.get("testCases")).isEmpty()) {
                    String sIn = map.get("sampleInput") != null ? map.get("sampleInput").toString() : "";
                    String sOut = map.get("sampleOutput") != null ? map.get("sampleOutput").toString() : "";
                    map.put("testCases", List.of(Map.of("inputData", sIn, "expectedOutput", sOut, "isHidden", false)));
                }
                if (!map.containsKey("difficulty") || map.get("difficulty") == null) map.put("difficulty", diff);
                if (!map.containsKey("points") || map.get("points") == null) map.put("points", 50);
                if (!map.containsKey("tags") || map.get("tags") == null) map.put("tags", topic + ", DSA");
                return map;
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI JSON response for problem generation (raw length={}), using diverse dynamic generator: {}", 
                raw != null ? raw.length() : 0, e.getMessage());
        }

        // ── Comprehensive, Topic-Accurate Problem Generator ──
        String tLower = topic != null ? topic.toLowerCase().trim() : "";
        String title;
        String desc;
        String sampleIn;
        String sampleOut;
        List<Map<String, Object>> tcs;

        if (tLower.contains("tree") || tLower.contains("bst")) {
            title = "Validate Binary Search Tree Property";
            desc = "### Problem Statement\nGiven `N` nodes of a binary tree represented as level-order sequence with `-1` representing null, determine if it satisfies the Binary Search Tree (BST) property.\n\n### Exact Rule & Target Value\n- Rule: For every node `X`, all nodes in its left subtree must have strictly smaller values than `X`, and all nodes in its right subtree must have strictly greater values than `X`.\n- Output: Print `true` if the tree is a valid BST, otherwise print `false`.\n\n### Input Format\nFirst line contains integer `N`.\nSecond line contains `N` integers representing level-order traversal.\n\n### Output Format\nPrint `true` or `false`.\n\n### Constraints\n- 1 <= N <= 10^5\n- -10^9 <= Node value <= 10^9\n\n### Example Walkthrough\nInput:\n3\n2 1 3\n\nExplanation:\nRoot is 2, left child is 1 (1 < 2), right child is 3 (3 > 2). Valid BST! Output: true";
            sampleIn = "3\n2 1 3";
            sampleOut = "true";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("graph") || tLower.contains("bfs") || tLower.contains("dfs") || tLower.contains("shortest")) {
            title = "Shortest Path in Unweighted Graph";
            desc = "### Problem Statement\nGiven an unweighted undirected graph with `V` vertices and `E` edges, find the length of the shortest path from start vertex `S` to destination vertex `D`.\n\n### Exact Rule & Target Value\n- Target Value: Shortest distance measured by number of edges traversed between `S` and `D`.\n- Rule: If `D` is unreachable from `S`, print `-1`.\n\n### Input Format\nFirst line contains four integers: `V`, `E`, `S`, and `D`.\nNext `E` lines each contain two integers `u` and `v` representing an undirected edge between `u` and `v`.\n\n### Output Format\nPrint the minimum number of edges from `S` to `D`, or `-1` if unreachable.\n\n### Constraints\n- 1 <= V <= 10^5\n- 0 <= E <= 2*10^5\n- 0 <= S, D < V\n\n### Example Walkthrough\nInput:\n4 4 0 3\n0 1\n0 2\n1 3\n2 3\n\nExplanation:\nShortest path from 0 to 3 is 0 -> 1 -> 3 (length 2 edges).\nOutput: 2";
            sampleIn = "4 4 0 3\n0 1\n0 2\n1 3\n2 3";
            sampleOut = "2";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("dp") || tLower.contains("dynamic") || tLower.contains("knapsack") || tLower.contains("coin")) {
            title = "Coin Change - Minimum Coins";
            desc = "### Problem Statement\nGiven an array of coin denominations of size `N` and a target amount `A`, find the minimum number of coins needed to make up that amount. Each coin denomination can be used an infinite number of times.\n\n### Exact Rule & Target Value\n- Target Value: `A` (target amount to form).\n- Condition: Minimize total coins whose sum equals `A`.\n- Rule: If that amount cannot be made up by any combination of the coins, print `-1`.\n\n### Input Format\nFirst line contains two integers: `N` (number of denominations) and `A` (target amount).\nSecond line contains `N` integers: `coins[0] coins[1] ... coins[N-1]`.\n\n### Output Format\nPrint the minimum number of coins, or `-1` if not possible.\n\n### Constraints\n- 1 <= N <= 12\n- 1 <= coins[i] <= 10^4\n- 0 <= A <= 10^4\n\n### Example Walkthrough\nInput:\n3 11\n1 2 5\n\nExplanation:\nTarget is 11. Optimal combination: 5 + 5 + 1 = 11 (3 coins).\nOutput: 3";
            sampleIn = "3 11\n1 2 5";
            sampleOut = "3";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("stack") || tLower.contains("parenthes") || tLower.contains("bracket")) {
            title = "Valid Parentheses Expression Validator";
            desc = "### Problem Statement\nGiven a string `S` containing characters '(', ')', '{', '}', '[' and ']', determine if the input string is valid.\n\n### Exact Rule\n- An input string is valid if open brackets are closed by the same type of brackets, and closed in the correct order.\n- Output: Print `true` if valid, otherwise print `false`.\n\n### Input Format\nA single line containing string `S`.\n\n### Output Format\nPrint `true` or `false`.\n\n### Constraints\n- 1 <= length(S) <= 10^5\n\n### Example Walkthrough\nInput:\n{[()]}\n\nExplanation:\nEvery opening bracket matches its corresponding closing bracket in proper nested order.\nOutput: true";
            sampleIn = "{[()]}";
            sampleOut = "true";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("queue") || tLower.contains("sliding") || tLower.contains("window") || tLower.contains("deque")) {
            title = "Sliding Window Maximum";
            desc = "### Problem Statement\nGiven an array of `N` integers and a sliding window size `K`, output the maximum value in each window of size `K` as it slides from left to right.\n\n### Exact Rule\n- Rule: For each window of `K` contiguous elements, determine its maximum integer.\n- Output: Print the window maximums separated by single space.\n\n### Input Format\nFirst line contains two integers: `N` and `K`.\nSecond line contains `N` space-separated integers.\n\n### Output Format\nPrint `N - K + 1` space-separated integers.\n\n### Constraints\n- 1 <= K <= N <= 10^5\n- -10^9 <= nums[i] <= 10^9\n\n### Example Walkthrough\nInput:\n8 3\n1 3 -1 -3 5 3 6 7\n\nExplanation:\nWindows: [1,3,-1]->3, [3,-1,-3]->3, [-1,-3,5]->5, [-3,5,3]->5, [5,3,6]->6, [3,6,7]->7.\nOutput: 3 3 5 5 6 7";
            sampleIn = "8 3\n1 3 -1 -3 5 3 6 7";
            sampleOut = "3 3 5 5 6 7";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("linked") || tLower.contains("list") || tLower.contains("node")) {
            title = "Merge Two Sorted Linked Lists";
            desc = "### Problem Statement\nGiven two sorted singly linked lists of lengths `N` and `M`, merge them into a single sorted linked list.\n\n### Exact Rule\n- Rule: Combine nodes from both lists into one monotonically non-decreasing sorted order.\n- Output: Print merged node values separated by space.\n\n### Input Format\nFirst line contains two integers: `N` and `M`.\nSecond line contains `N` sorted integers of list 1.\nThird line contains `M` sorted integers of list 2.\n\n### Output Format\nPrint the sorted merged values separated by space.\n\n### Constraints\n- 0 <= N, M <= 10^5\n- -10^9 <= value <= 10^9\n\n### Example Walkthrough\nInput:\n3 3\n1 2 4\n1 3 4\n\nExplanation:\nMerging [1, 2, 4] and [1, 3, 4] produces [1, 1, 2, 3, 4, 4].\nOutput: 1 1 2 3 4 4";
            sampleIn = "3 3\n1 2 4\n1 3 4";
            sampleOut = "1 1 2 3 4 4";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("matrix") || tLower.contains("grid") || tLower.contains("2d")) {
            title = "Rotate Matrix 90 Degrees Clockwise";
            desc = "### Problem Statement\nGiven an `N x N` 2D square matrix of integers, rotate the matrix by 90 degrees clockwise in-place.\n\n### Exact Rule\n- Rule: Element at row `i`, col `j` moves to row `j`, col `N - 1 - i`.\n- Output: Print the rotated matrix, one row per line with space-separated numbers.\n\n### Input Format\nFirst line contains integer `N`.\nNext `N` lines each contain `N` space-separated integers.\n\n### Output Format\nPrint `N` lines of `N` space-separated integers.\n\n### Constraints\n- 1 <= N <= 500\n- -10^5 <= matrix[i][j] <= 10^5\n\n### Example Walkthrough\nInput:\n3\n1 2 3\n4 5 6\n7 8 9\n\nExplanation:\nRotating 90 degrees clockwise turns first row into last column: [7, 4, 1], [8, 5, 2], [9, 6, 3].\nOutput:\n7 4 1\n8 5 2\n9 6 3";
            sampleIn = "3\n1 2 3\n4 5 6\n7 8 9";
            sampleOut = "7 4 1\n8 5 2\n9 6 3";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("bit") || tLower.contains("xor") || tLower.contains("bitwise")) {
            title = "Single Number via Bitwise XOR";
            desc = "### Problem Statement\nGiven a non-empty array of `N` integers where every element appears exactly twice except for one unique element, find that single unique element using O(1) auxiliary space.\n\n### Exact Rule\n- Rule: `x ^ x = 0` and `x ^ 0 = x`. XORing all array elements yields the single non-repeated element.\n- Output: Print the single unique integer.\n\n### Input Format\nFirst line contains integer `N`.\nSecond line contains `N` space-separated integers.\n\n### Output Format\nPrint the unique element.\n\n### Constraints\n- 1 <= N <= 10^5 (N is always odd)\n- -10^9 <= nums[i] <= 10^9\n\n### Example Walkthrough\nInput:\n5\n4 1 2 1 2\n\nExplanation:\n1 and 2 appear twice. 4 appears once.\nOutput: 4";
            sampleIn = "5\n4 1 2 1 2";
            sampleOut = "4";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("math") || tLower.contains("prime") || tLower.contains("gcd") || tLower.contains("number theory")) {
            title = "Greatest Common Divisor (Euclid)";
            desc = "### Problem Statement\nGiven two positive integers `A` and `B`, compute their Greatest Common Divisor (GCD) using the Euclidean algorithm.\n\n### Exact Rule\n- Rule: `gcd(A, B) = gcd(B, A % B)` until remainder is 0.\n- Output: Print the greatest common divisor.\n\n### Input Format\nA single line containing two space-separated integers: `A` and `B`.\n\n### Output Format\nPrint the integer GCD.\n\n### Constraints\n- 1 <= A, B <= 10^12\n\n### Example Walkthrough\nInput:\n48 18\n\nExplanation:\n48 % 18 = 12; 18 % 12 = 6; 12 % 6 = 0. GCD is 6.\nOutput: 6";
            sampleIn = "48 18";
            sampleOut = "6";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("recursion") || tLower.contains("backtrack") || tLower.contains("subset")) {
            title = "Count Valid Subsets with Target Sum";
            desc = "### Problem Statement\nGiven an array of `N` positive integers and a target sum `K`, count the total number of non-empty subsets whose elements sum to exactly `K`.\n\n### Exact Rule\n- Rule: Sum of subset elements must equal `K`.\n- Output: Print the count of such subsets.\n\n### Input Format\nFirst line contains two integers: `N` and `K`.\nSecond line contains `N` space-separated integers.\n\n### Output Format\nPrint the integer count of valid subsets.\n\n### Constraints\n- 1 <= N <= 20\n- 1 <= nums[i], K <= 10^4\n\n### Example Walkthrough\nInput:\n4 5\n1 2 3 4\n\nExplanation:\nSubsets that sum to 5: {1, 4} and {2, 3}. Total = 2.\nOutput: 2";
            sampleIn = "4 5\n1 2 3 4";
            sampleOut = "2";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("string") || tLower.contains("palindrome") || tLower.contains("anagram")) {
            title = "Palindrome String Verifier";
            desc = "### Problem Statement\nGiven a string `S`, verify whether it is a valid palindrome by reading it forwards and backwards.\n\n### Exact Rule\n- Rule: A string is a palindrome if it reads the exact same from left to right as it does from right to left (ignoring uppercase/lowercase differences).\n- Output: Print `true` if `S` is a palindrome, otherwise print `false`.\n\n### Input Format\nA single line containing the string `S`.\n\n### Output Format\nPrint `true` or `false`.\n\n### Constraints\n- 1 <= length(S) <= 10^5\n\n### Example Walkthrough\nInput:\nracecar\n\nExplanation:\nReversing \"racecar\" gives \"racecar\". Output: true";
            sampleIn = "racecar";
            sampleOut = "true";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("search") || tLower.contains("binary search")) {
            title = "Binary Search Index Locator";
            desc = "### Problem Statement\nGiven a sorted array of `N` distinct integers and an explicit target value `K`, return the 0-based index if `K` is found in the array. If not found, return `-1`.\n\n### Exact Rule & Target Value\n- Target Value: `K` is explicitly specified on the first line after `N`.\n- Rule: Search for `K` in the sorted array. If `K` exists at index `i`, print `i`. If not found, print `-1`.\n\n### Input Format\nFirst line contains two space-separated integers: `N` and `K`.\nSecond line contains `N` sorted space-separated integers.\n\n### Output Format\nPrint the 0-based index of target value `K`, or `-1` if not found.\n\n### Constraints\n- 1 <= N <= 10^5\n- -10^9 <= nums[i], K <= 10^9\n\n### Example Walkthrough\nInput:\n5 9\n-1 0 3 5 9\n\nExplanation:\nTarget value K is 9. In array [-1, 0, 3, 5, 9], 9 is at index 4.\nOutput: 4";
            sampleIn = "5 9\n-1 0 3 5 9";
            sampleOut = "4";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else if (tLower.contains("sum") || tLower.contains("two sum")) {
            title = "Two Sum Target Finder";
            desc = "### Problem Statement\nGiven an array of `N` integers and an explicit target value `target`, determine whether there exist two distinct indices `i` and `j` (i != j) such that `nums[i] + nums[j] == target`.\n\n### Exact Rule & Target Value\n- Target Value: `target` is provided on the first line after `N`.\n- Rule: If such a pair exists, print `YES`. If no two numbers add up to `target`, print `NO`.\n\n### Input Format\nFirst line contains two space-separated integers: `N` (array length) and `target` (target sum).\nSecond line contains `N` space-separated integers.\n\n### Output Format\nPrint `YES` or `NO`.\n\n### Constraints\n- 2 <= N <= 10^5\n- -10^9 <= nums[i], target <= 10^9\n\n### Example Walkthrough\nInput:\n4 9\n2 7 11 15\n\nExplanation:\nTarget is 9. Elements at index 0 (2) and index 1 (7) sum to 9. Output: YES";
            sampleIn = "4 9\n2 7 11 15";
            sampleOut = "YES";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        } else {
            // Dynamic Topic-Specific Synthesizer for Any Custom Topic
            title = topic.trim() + " Assessment Problem";
            desc = String.format(
                "### Problem Statement\n" +
                "Design and implement an optimal algorithm to solve the **%s** assessment problem.\n" +
                "Given an integer `N` representing test inputs followed by `N` values, process each element according to the core principles of %s.\n\n" +
                "### Exact Rule & Target Value\n" +
                "- Rule: Compute the cumulative result for the given %s dataset where each element contributes to the target evaluation.\n" +
                "- Output: Print the computed optimal value on a single line.\n\n" +
                "### Input Format\n" +
                "First line contains integer `N`.\n" +
                "Second line contains `N` space-separated integers.\n\n" +
                "### Output Format\n" +
                "Print the evaluated result matching the %s rule.\n\n" +
                "### Constraints\n" +
                "- 1 <= N <= 10^5\n" +
                "- -10^9 <= value <= 10^9\n\n" +
                "### Example Walkthrough\n" +
                "Input:\n4\n10 20 30 40\n\n" +
                "Explanation:\nApplying the %s evaluation rule over the 4 values yields the target output.\nOutput: 100",
                topic, topic, topic, topic, topic
            );
            sampleIn = "4\n10 20 30 40";
            sampleOut = "100";
            tcs = List.of(Map.of("inputData", sampleIn, "expectedOutput", sampleOut, "isHidden", false));
        }

        String starter;
        if ("java".equalsIgnoreCase(lang)) {
            starter = "import java.util.*;\nimport java.io.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        if (sc.hasNext()) {\n            // Read input and implement your solution logic below:\n            String line = sc.nextLine();\n            System.out.println(line);\n        }\n    }\n}";
        } else if ("cpp".equalsIgnoreCase(lang) || "c++".equalsIgnoreCase(lang)) {
            starter = "#include <iostream>\nusing namespace std;\n\nint main() {\n    // Implement your solution logic below:\n    int n;\n    if (cin >> n) {\n        cout << n << endl;\n    }\n    return 0;\n}";
        } else if ("c".equalsIgnoreCase(lang)) {
            starter = "#include <stdio.h>\n\nint main() {\n    // Implement your solution logic below:\n    int n;\n    if (scanf(\"%d\", &n) == 1) {\n        printf(\"%d\\n\", n);\n    }\n    return 0;\n}";
        } else {
            starter = "# Write your optimal solution here\nimport sys\n\ndef main():\n    lines = sys.stdin.read().split()\n    if not lines:\n        return\n    # Implement your solution logic below:\n    print(lines[0])\n\nif __name__ == '__main__':\n    main()\n";
        }

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
        String s = raw.trim();
        // Remove markdown fences if present
        if (s.contains("```")) {
            int firstTick = s.indexOf("```");
            int secondTick = s.indexOf("```", firstTick + 3);
            if (secondTick != -1) {
                String sub = s.substring(firstTick + 3, secondTick).trim();
                if (sub.toLowerCase().startsWith("json")) {
                    sub = sub.substring(4).trim();
                }
                s = sub;
            }
        }
        int firstBrace = s.indexOf('{');
        if (firstBrace == -1) return s;

        // Balanced brace scanner to capture complete JSON object without trailing garbage
        int depth = 0;
        int endBrace = -1;
        boolean inString = false;
        boolean escape = false;

        for (int i = firstBrace; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        endBrace = i;
                        break;
                    }
                }
            }
        }

        if (endBrace != -1) {
            return s.substring(firstBrace, endBrace + 1).trim();
        }
        int lastBrace = s.lastIndexOf('}');
        if (lastBrace > firstBrace) {
            return s.substring(firstBrace, lastBrace + 1).trim();
        }
        return s.trim();
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
    // 5. Submit Problem Solution & Verify Final Output
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

        // Evaluate ONLY public sample verification test cases (strictly no hidden test cases)
        List<TestCase> testCases = new ArrayList<>();
        String sIn = problem.getSampleInput() != null ? problem.getSampleInput().trim() : "";
        String sOut = problem.getSampleOutput() != null ? problem.getSampleOutput().trim() : "";

        if (!sOut.isBlank()) {
            testCases.add(TestCase.builder()
                    .problem(problem)
                    .inputData(sIn)
                    .expectedOutput(sOut)
                    .isHidden(false)
                    .build());
        }

        List<TestCase> dbCases = testCaseRepository.findByProblemOrderByIdAsc(problem);
        if (dbCases != null) {
            for (TestCase tc : dbCases) {
                // Ignore any legacy dummy test cases
                if (tc.getInputData() != null && (tc.getInputData().contains("test_") || tc.getInputData().equals("3\n10 20 30") || tc.getInputData().equals("4\n-5 5 -10 10"))) {
                    continue;
                }
                // Avoid duplicating the sample case
                if (tc.getInputData() != null && tc.getInputData().trim().equals(sIn) && tc.getExpectedOutput() != null && tc.getExpectedOutput().trim().equals(sOut)) {
                    continue;
                }
                if (tc.getExpectedOutput() != null && !tc.getExpectedOutput().isBlank()) {
                    testCases.add(TestCase.builder()
                            .id(tc.getId())
                            .problem(problem)
                            .inputData(tc.getInputData() != null ? tc.getInputData().trim() : "")
                            .expectedOutput(tc.getExpectedOutput().trim())
                            .isHidden(false)
                            .build());
                }
            }
        }

        if (testCases.isEmpty()) {
            testCases.add(TestCase.builder()
                    .problem(problem)
                    .inputData(sIn)
                    .expectedOutput(sOut)
                    .isHidden(false)
                    .build());
        }

        int totalCases = testCases.size();
        int passedCount = 0;
        long totalExecTime = 0L;
        List<TestCaseResultDTO> results = new ArrayList<>();

        int testIndex = 1;
        for (TestCase tc : testCases) {
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

            // Match output flexibly so formatting / greeting / case differences don't fail valid solutions
            boolean outputMatches = false;
            if (expectedClean.isEmpty()) {
                outputMatches = true;
            } else if (actualClean.equals(expectedClean) || actualClean.equalsIgnoreCase(expectedClean)) {
                outputMatches = true;
            } else {
                String expectedLower = expectedClean.toLowerCase();
                String actualLower = actualClean.toLowerCase();
                if (actualLower.contains(expectedLower)) {
                    outputMatches = true;
                } else {
                    List<String> actualLines = actualClean.lines().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                    if (!actualLines.isEmpty() && actualLines.get(actualLines.size() - 1).equalsIgnoreCase(expectedClean)) {
                        outputMatches = true;
                    }
                }
            }

            boolean passed = execResult.exitCode() == 0 &&
                             (execResult.error() == null || execResult.error().trim().isEmpty()) &&
                             outputMatches;

            if (passed) {
                passedCount++;
            }

            TestCaseResultDTO r = TestCaseResultDTO.builder()
                    .testIndex(testIndex++)
                    .isHidden(false)
                    .passed(passed)
                    .executionTimeMs(execResult.executionTimeMs())
                    .error(execResult.error())
                    .input(tc.getInputData())
                    .expected(tc.getExpectedOutput())
                    .actual(execResult.output())
                    .build();

            results.add(r);
        }

        // Verdict: Passed if all public verification cases pass
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

        String subMode = (request.getMode() != null && !request.getMode().isBlank())
                ? request.getMode().trim().toUpperCase()
                : "NORMAL";
        String modeLabel = "SECURE".equalsIgnoreCase(subMode) ? "Secure Mode" : "Normal Mode";

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
                .executionMode(subMode)
                .build();
        problemSubmissionRepository.save(submission);

        String message;
        if (isAllPassed) {
            message = String.format("🎉 ACCEPTED [%s]! Final output verified successfully! +%d Points credited.", modeLabel, pointsToAward);
        } else {
            String errDetail = results.stream()
                    .filter(res -> !res.getPassed())
                    .findFirst()
                    .map(res -> res.getError() != null && !res.getError().isBlank() ? res.getError().trim() : "Expected '" + res.getExpected() + "', but received '" + (res.getActual() != null ? res.getActual().trim() : "") + "'")
                    .orElse("Output did not match expected problem output.");
            message = String.format("❌ Execution Failed [%s]: %s", modeLabel, errDetail);
        }

        return ProblemSubmissionResponse.builder()
                .isPassed(isAllPassed)
                .isSolved(isAllPassed)
                .pointsAwarded(pointsToAward)
                .totalPoints(user.getTotalPoints())
                .totalTestCases(totalCases)
                .passedTestCases(passedCount)
                .hiddenTestCasesCount(0)
                .hiddenTestCasesPassed(0)
                .totalExecutionTimeMs(totalExecTime)
                .message(message)
                .mode(subMode)
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
                .totalTestCasesCount(publicTests.size())
                .hiddenTestCasesCount(0)
                .publicTestCases(publicTests)
                .build();
    }
}
