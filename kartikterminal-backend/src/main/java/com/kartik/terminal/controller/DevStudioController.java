package com.kartik.terminal.controller;

import com.kartik.terminal.service.DevStudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/devstudio")
@RequiredArgsConstructor
public class DevStudioController {

    private final DevStudioService devStudioService;

    @PostMapping("/coding-agent")
    public ResponseEntity<Map<String, Object>> runCodingAgent(@RequestBody Map<String, String> payload) {
        String prompt = payload.getOrDefault("prompt", "Implement an LRU Cache with O(1) get and put operations.");
        String language = payload.getOrDefault("language", "Java");
        String starterCode = payload.getOrDefault("starterCode", "");
        return ResponseEntity.ok(devStudioService.runCodingAgent(prompt, language, starterCode));
    }

    @PostMapping("/time-travel")
    public ResponseEntity<Map<String, Object>> getTimeTravelTrace(@RequestBody Map<String, String> payload) {
        String code = payload.getOrDefault("code", "int sum = 0;\nfor(int i = 1; i <= 5; i++) {\n    sum += i;\n    System.out.println(sum);\n}");
        String language = payload.getOrDefault("language", "Java");
        String customInput = payload.getOrDefault("customInput", "");
        return ResponseEntity.ok(devStudioService.generateTimeTravelTrace(code, language, customInput));
    }

    @PostMapping("/visualize")
    public ResponseEntity<Map<String, Object>> getExecutionVisualization(@RequestBody Map<String, String> payload) {
        String code = payload.getOrDefault("code", "List<String> list = new ArrayList<>();\nlist.add(\"Kartik\");\nlist.add(\"Terminal\");");
        String language = payload.getOrDefault("language", "Java");
        return ResponseEntity.ok(devStudioService.generateExecutionVisualization(code, language));
    }

    @PostMapping("/test-generator")
    public ResponseEntity<Map<String, Object>> generateAutomaticTests(@RequestBody Map<String, String> payload) {
        String code = payload.getOrDefault("code", "public int divide(int a, int b) {\n    if (b == 0) throw new IllegalArgumentException(\"Div by zero\");\n    return a / b;\n}");
        String language = payload.getOrDefault("language", "Java");
        String framework = payload.getOrDefault("framework", "JUnit 5");
        return ResponseEntity.ok(devStudioService.generateAutomaticTests(code, language, framework));
    }

    @PostMapping("/mutation-testing")
    public ResponseEntity<Map<String, Object>> runMutationTesting(@RequestBody Map<String, String> payload) {
        String code = payload.getOrDefault("code", "public boolean isPositive(int n) {\n    return n > 0;\n}");
        String language = payload.getOrDefault("language", "Java");
        String testSuite = payload.getOrDefault("testSuite", "@Test void test() { assertTrue(isPositive(5)); }");
        return ResponseEntity.ok(devStudioService.runMutationTesting(code, language, testSuite));
    }

    @PostMapping("/architecture")
    public ResponseEntity<Map<String, Object>> generateArchitecture(@RequestBody Map<String, String> payload) {
        String prompt = payload.getOrDefault("prompt", "High-throughput Real-time Code Execution Platform with Sandboxing");
        String style = payload.getOrDefault("style", "Event-Driven Microservices");
        String cloudProvider = payload.getOrDefault("cloudProvider", "AWS / Kubernetes");
        return ResponseEntity.ok(devStudioService.generateArchitecture(prompt, style, cloudProvider));
    }

    @PostMapping("/algorithm-comparator")
    public ResponseEntity<Map<String, Object>> compareAlgorithms(@RequestBody Map<String, Object> payload) {
        String category = payload.getOrDefault("category", "Sorting").toString();
        String algorithms = payload.getOrDefault("algorithms", "QuickSort, MergeSort, HeapSort, TimSort").toString();
        String datasetType = payload.getOrDefault("datasetType", "Random Distribution").toString();
        int dataSize = payload.containsKey("dataSize") ? Integer.parseInt(payload.get("dataSize").toString()) : 5000;
        return ResponseEntity.ok(devStudioService.compareAlgorithms(category, algorithms, datasetType, dataSize));
    }

    @PostMapping("/data-structure")
    public ResponseEntity<Map<String, Object>> generateDataStructureStep(@RequestBody Map<String, String> payload) {
        String dataStructure = payload.getOrDefault("dataStructure", "BST");
        String operation = payload.getOrDefault("operation", "INSERT");
        String value = payload.getOrDefault("value", "25");
        String currentStateJson = payload.getOrDefault("currentState", "{}");
        return ResponseEntity.ok(devStudioService.generateDataStructureStep(dataStructure, operation, value, currentStateJson));
    }
}
