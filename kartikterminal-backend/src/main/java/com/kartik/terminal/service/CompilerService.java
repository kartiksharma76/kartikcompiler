package com.kartik.terminal.service;

import com.kartik.terminal.dto.CompilerDTOs.*;
import com.kartik.terminal.entity.ExecutionRecord;
import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.ExecutionRecordRepository;
import com.kartik.terminal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilerService {

    private final ExecutionRecordRepository executionRecordRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${app.compiler.temp-dir:/tmp/kartik_compiler}")
    private String tempDir;

    @Value("${app.compiler.timeout:10}")
    private int timeoutSeconds;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.compiler.max-output:50000}")
    private int maxOutputBytes;

    // ========== MAIN EXECUTE METHOD ==========
    @Transactional
    public ExecutionResponse executeCode(CodeRequest request) {
        User user = authService.getCurrentUser();
        if (Boolean.TRUE.equals(user.getIsDisqualified()) || Boolean.FALSE.equals(user.getIsActive()) || (user.getCheatViolations() != null && user.getCheatViolations() >= 3)) {
            throw new RuntimeException("Account is locked due to Anti-Cheat violations. Only an Administrator can unlock your account.");
        }
        long startTime = System.currentTimeMillis();

        ExecutionResult result = runCodeInSandbox(request);
        long execTime = System.currentTimeMillis() - startTime;

        boolean success = result.exitCode == 0 && result.error.isEmpty();
        ExecutionRecord.ExecutionStatus status = determineStatus(result, execTime);
        int points = ExecutionRecord.calculatePoints(success, execTime, request.getLanguage());

        // Save execution record
        ExecutionRecord record = ExecutionRecord.builder()
                .user(user)
                .language(request.getLanguage())
                .code(request.getCode())
                .input(request.getInput())
                .output(truncate(result.output, maxOutputBytes))
                .errorOutput(truncate(result.error, maxOutputBytes))
                .success(success)
                .executionTimeMs(execTime)
                .status(status)
                .points(success ? points : 0)
                .title(request.getTitle())
                .build();

        ExecutionRecord saved = executionRecordRepository.save(record);

        // Update user stats
        userRepository.updateExecutionStats(
                user.getId(),
                success ? 1 : 0,
                success ? points : 0,
                execTime,
                request.getLanguage()
        );

        log.info("Code executed: user={}, lang={}, success={}, time={}ms",
                user.getUsername(), request.getLanguage(), success, execTime);

        return ExecutionResponse.builder()
                .output(success ? result.output : "")
                .error(success ? "" : result.error)
                .success(success)
                .executionTimeMs(execTime)
                .language(request.getLanguage())
                .points(success ? points : 0)
                .status(status.name())
                .executedAt(LocalDateTime.now())
                .recordId(saved.getId())
                .build();
    }

    // ========== SANDBOX EXECUTION ==========
    private ExecutionResult runCodeInSandbox(CodeRequest request) {
        String sessionId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Path workDir = null;

        try {
            workDir = Files.createTempDirectory(Path.of(tempDir), "run_" + sessionId + "_");
            workDir.toFile().setWritable(true);

            return switch (request.getLanguage().toLowerCase()) {
                case "java"       -> runJava(workDir, request.getCode(), request.getInput());
                case "python", "py" -> runPython(workDir, request.getCode(), request.getInput());
                case "cpp", "c++" -> runCpp(workDir, request.getCode(), request.getInput());
                case "c"          -> runC(workDir, request.getCode(), request.getInput());
                case "js", "node" -> runNode(workDir, request.getCode(), request.getInput());
                case "go", "golang" -> runGo(workDir, request.getCode(), request.getInput());
                case "mysql", "sql" -> runMySql(request.getCode());
                case "ts", "typescript" -> runTypeScript(workDir, request.getCode(), request.getInput());
                case "rust", "rs" -> runRust(workDir, request.getCode(), request.getInput());
                case "php"        -> runPhp(workDir, request.getCode(), request.getInput());
                case "ruby", "rb" -> runRuby(workDir, request.getCode(), request.getInput());
                case "bash", "sh" -> runBash(workDir, request.getCode(), request.getInput());
                default           -> new ExecutionResult("", "Unsupported language: " + request.getLanguage(), 1);
            };

        } catch (IOException e) {
            log.error("Failed to create temp directory", e);
            return new ExecutionResult("", "Internal server error: " + e.getMessage(), 1);
        } finally {
            // Clean up temp files
            if (workDir != null) {
                deleteDirectory(workDir.toFile());
            }
        }
    }

    // ========== JAVA ==========
    private ExecutionResult runJava(Path dir, String code, String input) throws IOException {
        // Extract class name
        String className = extractJavaClassName(code);
        Path srcFile = dir.resolve(className + ".java");
        Files.writeString(srcFile, code);

        // Compile
        ExecutionResult compileResult = runProcess(
                new String[]{"javac", srcFile.toString()},
                dir, "", 30
        );
        if (compileResult.exitCode != 0) {
            return new ExecutionResult("", "Compilation Error:\n" + compileResult.error, 1);
        }

        // Run with security restrictions
        return runProcess(
                new String[]{
                    "java",
                    "-cp", dir.toString(),
                    "-Xmx128m",        // limit memory
                    "-Xss512k",        // limit stack
                    className
                },
                dir, input, timeoutSeconds
        );
    }

    // ========== PYTHON ==========
    private ExecutionResult runPython(Path dir, String code, String input) throws IOException {
        Path srcFile = dir.resolve("main.py");
        Files.writeString(srcFile, code);
        return runProcess(
                new String[]{"python3", srcFile.toString()},
                dir, input, timeoutSeconds
        );
    }

    // ========== C++ ==========
    private ExecutionResult runCpp(Path dir, String code, String input) throws IOException {
        Path srcFile = dir.resolve("main.cpp");
        Path binFile = dir.resolve("main_out");
        Files.writeString(srcFile, code);

        ExecutionResult compileResult = runProcess(
                new String[]{"g++", "-O2", "-o", binFile.toString(), srcFile.toString()},
                dir, "", 30
        );
        if (compileResult.exitCode != 0) {
            return new ExecutionResult("", "Compilation Error:\n" + compileResult.error, 1);
        }
        return runProcess(new String[]{binFile.toString()}, dir, input, timeoutSeconds);
    }

    // ========== C ==========
    private ExecutionResult runC(Path dir, String code, String input) throws IOException {
        Path srcFile = dir.resolve("main.c");
        Path binFile = dir.resolve("main_out");
        Files.writeString(srcFile, code);

        ExecutionResult compileResult = runProcess(
                new String[]{"gcc", "-o", binFile.toString(), srcFile.toString()},
                dir, "", 30
        );
        if (compileResult.exitCode != 0) {
            return new ExecutionResult("", "Compilation Error:\n" + compileResult.error, 1);
        }
        return runProcess(new String[]{binFile.toString()}, dir, input, timeoutSeconds);
    }

    // ========== NODE.JS ==========
    private ExecutionResult runNode(Path dir, String code, String input) throws IOException {
        Path srcFile = dir.resolve("main.js");
        Files.writeString(srcFile, code);
        return runProcess(
                new String[]{"node", srcFile.toString()},
                dir, input, timeoutSeconds
        );
    }

    // ========== GO ==========
    private ExecutionResult runGo(Path dir, String code, String input) throws IOException {
        Path srcFile = dir.resolve("main.go");
        Files.writeString(srcFile, code);
        return runProcess(
                new String[]{"go", "run", srcFile.toString()},
                dir, input, timeoutSeconds
        );
    }

    // ========== RUST ==========
    private ExecutionResult runRust(Path dir, String code, String input) throws IOException {
        Path srcFile = dir.resolve("main.rs");
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        Path binFile = dir.resolve(isWin ? "main_out.exe" : "main_out");
        Files.writeString(srcFile, code);

        ExecutionResult compileResult = runProcess(
                new String[]{"rustc", srcFile.toString(), "-o", binFile.toString()},
                dir, "", 30
        );
        if (compileResult.exitCode != 0) {
            return new ExecutionResult("", "Rust Compilation Error:\n" + compileResult.error, 1);
        }
        return runProcess(new String[]{binFile.toString()}, dir, input, timeoutSeconds);
    }

    // ========== PHP ==========
    private ExecutionResult runPhp(Path dir, String code, String input) throws IOException {
        Path srcFile = dir.resolve("main.php");
        Files.writeString(srcFile, code);
        return runProcess(
                new String[]{"php", "-f", srcFile.toString()},
                dir, input, timeoutSeconds
        );
    }

    // ========== RUBY ==========
    private ExecutionResult runRuby(Path dir, String code, String input) throws IOException {
        Path srcFile = dir.resolve("main.rb");
        Files.writeString(srcFile, code);
        return runProcess(
                new String[]{"ruby", srcFile.toString()},
                dir, input, timeoutSeconds
        );
    }

    // ========== BASH / SHELL ==========
    private ExecutionResult runBash(Path dir, String code, String input) throws IOException {
        boolean isWin = System.getProperty("os.name").toLowerCase().contains("win");
        if (isWin) {
            Path srcFile = dir.resolve("main.bat");
            Files.writeString(srcFile, code);
            return runProcess(
                    new String[]{"cmd.exe", "/c", srcFile.toString()},
                    dir, input, timeoutSeconds
            );
        } else {
            Path srcFile = dir.resolve("main.sh");
            Files.writeString(srcFile, code);
            return runProcess(
                    new String[]{"bash", srcFile.toString()},
                    dir, input, timeoutSeconds
            );
        }
    }

    // ========== PROCESS RUNNER ==========
    private ExecutionResult runProcess(String[] cmd, Path workDir, String input, int timeoutSecs) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Write input
            if (input != null && !input.isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(input.getBytes());
                    os.flush();
                }
            } else {
                process.getOutputStream().close();
            }

            // Read stdout and stderr concurrently
            Future<String> stdoutFuture = executor.submit(() -> readStream(process.getInputStream()));
            Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));

            boolean finished = process.waitFor(timeoutSecs, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return new ExecutionResult("", "⏱️ Execution Timeout: Code exceeded " + timeoutSecs + " second limit.", 124);
            }

            String stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
            String stderr = stderrFuture.get(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            return new ExecutionResult(
                    truncate(stdout, maxOutputBytes),
                    truncate(stderr, maxOutputBytes),
                    exitCode
            );

        } catch (Exception e) {
            return new ExecutionResult("", "Execution error: " + e.getMessage(), 1);
        } finally {
            executor.shutdownNow();
        }
    }

    private String readStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
                if (sb.length() > maxOutputBytes) {
                    sb.append("\n[Output truncated - max size exceeded]");
                    break;
                }
            }
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }

    // ========== HELPER METHODS ==========
    private String extractJavaClassName(String code) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("public\\s+class\\s+(\\w+)")
                .matcher(code);
        return m.find() ? m.group(1) : "Main";
    }

    private ExecutionRecord.ExecutionStatus determineStatus(ExecutionResult result, long execTime) {
        if (result.exitCode == 124) return ExecutionRecord.ExecutionStatus.TIMEOUT;
        if (result.exitCode == 0) return ExecutionRecord.ExecutionStatus.SUCCESS;
        if (result.error.contains("Compilation Error")) return ExecutionRecord.ExecutionStatus.COMPILE_ERROR;
        return ExecutionRecord.ExecutionStatus.RUNTIME_ERROR;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "\n[truncated]" : s;
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) for (File f : files) deleteDirectory(f);
        }
        dir.delete();
    }

    // Ensure temp dir exists
    @jakarta.annotation.PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Path.of(tempDir));
        log.info("Compiler temp directory: {}", tempDir);
    }

    // ========== MYSQL EXECUTOR ==========
    private Connection getSandboxConnection() throws SQLException {
        String sandboxUrl = dbUrl;
        if (sandboxUrl.contains("/kartikterminal")) {
            sandboxUrl = sandboxUrl.replace("/kartikterminal", "/kartik_sandbox");
        } else {
            int lastSlash = sandboxUrl.lastIndexOf('/');
            int queryStart = sandboxUrl.indexOf('?', lastSlash);
            if (queryStart != -1) {
                sandboxUrl = sandboxUrl.substring(0, lastSlash + 1) + "kartik_sandbox" + sandboxUrl.substring(queryStart);
            } else {
                sandboxUrl = sandboxUrl.substring(0, lastSlash + 1) + "kartik_sandbox";
            }
        }
        
        String serverUrl = dbUrl.substring(0, dbUrl.lastIndexOf('/') + 1);
        int queryIdx = dbUrl.indexOf('?');
        if (queryIdx != -1) {
            serverUrl += dbUrl.substring(queryIdx);
        }
        
        try (Connection conn = DriverManager.getConnection(serverUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS kartik_sandbox CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (Exception e) {
            log.warn("Could not pre-create kartik_sandbox database: {}", e.getMessage());
        }

        Connection sandboxConn = DriverManager.getConnection(sandboxUrl, dbUser, dbPassword);
        try (Statement stmt = sandboxConn.createStatement()) {
            stmt.execute("SET SESSION sql_mode = 'NO_ENGINE_SUBSTITUTION'");
        } catch (Exception e) {
            log.warn("Could not set sql_mode: {}", e.getMessage());
        }
        return sandboxConn;
    }

    private ExecutionResult runMySql(String code) {
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        int exitCode = 0;

        try (Connection conn = getSandboxConnection()) {
            // Basic SQL query splitter
            String[] queries = code.split(";");
            for (String query : queries) {
                query = query.trim();
                if (query.isEmpty()) continue;

                stdout.append("mysql> ").append(query).append(";\n");

                try (Statement stmt = conn.createStatement()) {
                    boolean hasResultSet = stmt.execute(query);
                    if (hasResultSet) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            formatResultSet(rs, stdout);
                        }
                    } else {
                        int updateCount = stmt.getUpdateCount();
                        stdout.append("Query OK, ").append(updateCount).append(" rows affected\n\n");
                    }
                } catch (SQLException e) {
                    stderr.append("ERROR ").append(e.getErrorCode()).append(" (").append(e.getSQLState()).append("): ").append(e.getMessage()).append("\n\n");
                    exitCode = 1;
                }
            }
        } catch (SQLException e) {
            stderr.append("Database Connection Error: ").append(e.getMessage()).append("\n");
            exitCode = 1;
        }

        return new ExecutionResult(stdout.toString(), stderr.toString(), exitCode);
    }

    private void formatResultSet(ResultSet rs, StringBuilder out) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int columns = md.getColumnCount();
        
        int[] widths = new int[columns];
        String[] headers = new String[columns];
        for (int i = 0; i < columns; i++) {
            headers[i] = md.getColumnLabel(i + 1);
            widths[i] = headers[i].length();
        }
        
        List<String[]> rows = new ArrayList<>();
        while (rs.next()) {
            String[] row = new String[columns];
            for (int i = 0; i < columns; i++) {
                Object val = rs.getObject(i + 1);
                row[i] = val == null ? "NULL" : val.toString();
                widths[i] = Math.max(widths[i], row[i].length());
            }
            rows.add(row);
        }
        
        StringBuilder border = new StringBuilder("+");
        for (int i = 0; i < columns; i++) {
            border.append("-".repeat(widths[i] + 2)).append("+");
        }
        border.append("\n");
        
        out.append(border);
        
        out.append("|");
        for (int i = 0; i < columns; i++) {
            out.append(" ").append(padRight(headers[i], widths[i])).append(" |");
        }
        out.append("\n").append(border);
        
        for (String[] row : rows) {
            out.append("|");
            for (int i = 0; i < columns; i++) {
                out.append(" ").append(padRight(row[i], widths[i])).append(" |");
            }
            out.append("\n");
        }
        out.append(border);
        out.append(rows.size()).append(" rows in set\n\n");
    }

    private String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    // ========== TYPESCRIPT EXECUTOR ==========
    private ExecutionResult runTypeScript(Path dir, String code, String input) throws IOException {
        Path srcFile = dir.resolve("main.ts");
        Path jsFile = dir.resolve("main.js");
        Files.writeString(srcFile, code);

        // 1. Attempt rapid transpilation with esbuild
        ExecutionResult transpileResult = runProcess(
                new String[]{resolveCmd("esbuild"), srcFile.toString(), "--outfile=" + jsFile.toString()},
                dir, "", 15
        );

        // 2. If global esbuild is not found, fallback to npx esbuild
        if (transpileResult.exitCode != 0 || !Files.exists(jsFile)) {
            transpileResult = runProcess(
                    new String[]{resolveCmd("npx"), "-y", "esbuild", srcFile.toString(), "--outfile=" + jsFile.toString()},
                    dir, "", 25
            );
        }

        // 3. Fallback to tsc if available
        if (transpileResult.exitCode != 0 || !Files.exists(jsFile)) {
            transpileResult = runProcess(
                    new String[]{resolveCmd("tsc"), srcFile.toString(), "--outFile", jsFile.toString(), "--target", "es2020"},
                    dir, "", 25
            );
        }

        if (transpileResult.exitCode != 0 && !Files.exists(jsFile)) {
            return new ExecutionResult("", "TypeScript Transpilation Error:\n" + transpileResult.error, 1);
        }

        // 4. Run compiled JavaScript using Node.js runtime
        return runProcess(
                new String[]{"node", jsFile.toString()},
                dir, input, timeoutSeconds
        );
    }

    private String resolveCmd(String cmd) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            if ("npx".equals(cmd)) return "npx.cmd";
            if ("npm".equals(cmd)) return "npm.cmd";
            if ("esbuild".equals(cmd)) return "esbuild.cmd";
            if ("tsc".equals(cmd)) return "tsc.cmd";
        }
        return cmd;
    }

    // ========== INNER RESULT CLASS ==========
    private record ExecutionResult(String output, String error, int exitCode) {}
}
