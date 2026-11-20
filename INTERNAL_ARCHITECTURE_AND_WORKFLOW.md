# 🏗️ KartikTerminal: Deep Internal Architecture & End-to-End Workflow

---

## 1. Executive Summary & System Overview

**KartikTerminal** is an enterprise-grade, cloud-native **Online Code Execution Sandbox, Automated Assessment Engine, and AI-Powered Academic Proctoring Ecosystem**. The platform is designed to serve four primary personas:
1. **Students / Independent Coders**: Write, compile, test, and submit algorithmic solutions, participate in proctored college exams, and track progress on real-time gamified leaderboards.
2. **College Admins & Faculty**: Manage institutional departments, create problem statements, schedule coding contests, monitor live student proctoring feeds, and audit student code submissions.
3. **Platform Super Admins**: Maintain global system health, approve/reject pending college registrations, inspect system-wide code execution metrics, and manage user privileges.
4. **Technical Recruiters & Evaluators**: Evaluate verified candidate code, inspect anti-cheat audits, review time-space complexity metrics, and analyze AI-driven candidate reports.

The platform is built on **Spring Boot 3.x (Java 17)**, **Spring Security 6.x**, **MySQL 8.x**, **JWT (HMAC-SHA256)**, **NVIDIA NIM AI Endpoints**, and a **Monaco-powered lightweight frontend**.

---

## 2. Layered Architecture Diagram

```mermaid
graph TD
    subgraph Client_Layer ["Client & Presentation Layer (Browser)"]
        UI_Editor["Monaco Code Editor (VS Code Core)"]
        UI_Proctor["TensorFlow.js & COCO-SSD Vision Engine"]
        UI_Audio["Web Audio API (Acoustic Surveillance)"]
        UI_Voice["Web Speech API (AI Voice Interview)"]
        UI_College["College ERP & Admin Command Center"]
        UI_Terminal["ANSI Terminal Emulation & Xterm Output"]
    end

    subgraph Security_Gateway ["Security & API Gateway Layer"]
        Filter_CORS["CORS Filter (Configurable Origins)"]
        Filter_JWT["JwtAuthenticationFilter (Bearer Token Claims)"]
        OAuth_Handler["OAuth2SuccessHandler / Google SSO"]
        RBAC_Guard["Spring Method Security (@PreAuthorize RBAC)"]
    end

    subgraph Business_Service ["Core Application & Service Layer"]
        Svc_Compiler["CompilerService (Subprocess Isolation & Sandbox)"]
        Svc_Problem["ProblemService (DSA Scoping & Test Runner)"]
        Svc_AI["AIService (NVIDIA NIM Gateway & Fallbacks)"]
        Svc_Auth["AuthService (User/College Registration & JWT)"]
        Svc_Proctor["AntiCheatService & ProctorController"]
        Svc_Dash["DashboardService & Gamification Rating Engine"]
        Svc_DevStudio["DevStudioService (Time-Travel & Architecture)"]
        Svc_Intel["IntelligenceSuiteService (10 AI Career Modules)"]
    end

    subgraph Persistence_Layer ["Data Persistence & Storage Layer"]
        DB_Pool["HikariCP Connection Pool"]
        ORM_JPA["Spring Data JPA / Hibernate 6.x"]
        DB_MySQL[("MySQL 8.0 Database (InnoDB Engine)")]
    end

    subgraph Execution_Sandbox ["OS Sandboxed Subprocess Environment"]
        FS_Temp["Temporary Disk Isolation (/tmp/kartik_compiler)"]
        Subprocess_OS["Native OS Subprocesses (java, python, g++, gcc, node)"]
        Watchdog["Execution Watchdog (10s Hard Timeout & Memory Limits)"]
    end

    subgraph External_AI ["External AI Provider Layer"]
        NVIDIA_API["NVIDIA NIM Cloud API (integrate.api.nvidia.com)"]
        Model_Laguna["poolside/laguna-xs-2.1"]
        Model_Nemotron["nvidia/nemotron-3.5-lightning-30b-a3b"]
        Model_Minimax["minimaxai/minimax-m3.1-flash"]
    end

    Client_Layer --> Security_Gateway
    Security_Gateway --> Business_Service
    Business_Service --> Persistence_Layer
    Persistence_Layer --> DB_Pool --> ORM_JPA --> DB_MySQL

    Svc_Compiler --> Execution_Sandbox
    Execution_Sandbox --> FS_Temp
    Execution_Sandbox --> Subprocess_OS
    Subprocess_OS --> Watchdog

    Svc_AI --> External_AI
    External_AI --> NVIDIA_API
    NVIDIA_API --> Model_Laguna
    NVIDIA_API --> Model_Nemotron
    NVIDIA_API --> Model_Minimax
```

---

## 3. Deep Architectural Components Breakdown

### 3.1 Client & Presentation Layer
* **Monaco Editor Integration**: Embedded VS Code editor providing intelligent code completion, bracket colorization, multi-theme support (*Dracula, Monokai, Dark+*), syntax highlighting, and keyboard shortcuts.
* **Proctoring Engine (TensorFlow.js + COCO-SSD)**: Runs locally on the client's GPU via WebGL. Detects multiple persons, mobile phones, books, and missing candidate faces without streaming raw heavy video to the server.
* **Audio Telemetry (Web Audio API)**: Uses `AudioContext` and `AnalyserNode` to compute root-mean-square (RMS) acoustic decibels, flagging continuous whispers or unauthorized voice exchanges.
* **Client-Side Auth Manager (`auth.js`)**: Intercepts outgoing `fetch` calls, dynamically injects `Authorization: Bearer <token>`, decodes JWT claims, and renders role-based interface elements.

#### 📊 Client-Side Edge AI Proctoring Pipeline Diagram:
```mermaid
flowchart LR
    subgraph Client_Hardware ["User Device Hardware"]
        CAM["Webcam Stream (640x480)"]
        MIC["Microphone Audio Stream"]
    end

    subgraph Browser_Edge_AI ["Browser Edge AI Engine (Client-Side)"]
        TFJS["TensorFlow.js (WebGL Accelerated)"]
        COCO["COCO-SSD MobileNet v2 Object Detector"]
        AUDIO_CTX["Web Audio API (AudioContext)"]
        ANALYSER["AnalyserNode (RMS Decibel Analysis)"]
        
        CAM --> TFJS --> COCO
        MIC --> AUDIO_CTX --> ANALYSER
    end

    subgraph Edge_Rule_Engine ["Real-Time Edge Evaluation (Every 1000ms)"]
        EVAL_PERSON{"Person Count == 1?"}
        EVAL_PHONE{"Phone Detected?"}
        EVAL_NOISE{"RMS Audio > Threshold?"}
        
        COCO --> EVAL_PERSON
        COCO --> EVAL_PHONE
        ANALYSER --> EVAL_NOISE
    end

    subgraph Client_Enforcement ["Immediate Local HUD Enforcement"]
        WARN["Red Screen Alert & Warning Audio Buzzer"]
        TAB_LOCK["Full-Screen & Tab-Switch Interceptor"]
    end

    subgraph Server_Audit ["Backend Telemetry (/api/proctor)"]
        REST_LOG["ProctorController / AntiCheatService"]
        DB_AUDIT[("MySQL: anti_cheat_logs")]
        AUTO_CUT{"Violations >= 5?"}
        TERMINATE["Auto-Cut Disqualification"]
    end

    EVAL_PERSON -- "Multiple or 0 Faces" --> WARN
    EVAL_PHONE -- "Confidence > 80%" --> WARN
    EVAL_NOISE -- "Sustained Speech" --> WARN
    
    WARN --> REST_LOG
    TAB_LOCK --> REST_LOG
    REST_LOG --> DB_AUDIT --> AUTO_CUT
    AUTO_CUT -- "Yes" --> TERMINATE
```

---

### 3.2 Security & Authentication Layer
* **Dual Authentication Modes**:
  1. **Google OAuth2**: `oauth2Login()` via Google Identity Services with automatic profile sync and user creation via `OAuth2SuccessHandler`.
  2. **Stateless JWT**: Standard username/email and BCrypt-hashed password authentication generating cryptographic HMAC-SHA256 tokens (`app.jwt.secret`, 24-hour expiration).
* **Role-Based Access Control (RBAC)**:
  * `SUPER_ADMIN` / `ADMIN`: Unrestricted global system access.
  * `COLLEGE_ADMIN` / `FACULTY`: College-scoped access (manage college students, view institution proctor feeds, assign problems, review exam submissions).
  * `STUDENT` / `USER`: Standard access (run compiler, solve assigned and public problems, view personalized dashboard and public leaderboards).
* **Multi-Tenancy Security**: College admins and faculty are strictly isolated to their own college (`currentUser.getInstitution().getId().equals(targetStudent.getInstitution().getId())`). Actions on foreign colleges or global admins are rejected with HTTP 403 Forbidden.

#### 📊 Security & Multi-Tenancy Request Filter Lifecycle:
```mermaid
flowchart TD
    subgraph Incoming_Traffic ["Client HTTP/HTTPS Traffic"]
        REQ["Incoming API Request (with Authorization: Bearer JWT)"]
    end

    subgraph Security_FilterChain ["Spring Security 6.x Filter Chain"]
        CORS["CorsFilter (Allowed Origins & Headers)"]
        CSRF["CSRF Disabled (Stateless API)"]
        JWT_FILTER["JwtAuthenticationFilter"]
        TOKEN_EXTRACT["Token Extraction & HMAC-SHA256 Secret Validation"]
        CLAIMS_PARSE["Claims Extractor (username, role, institutionId)"]
        USER_LOAD["UserDetailsService (Fetch User Principal)"]
        SEC_CTX["SecurityContextHolder (Set Authentication)"]
        
        REQ --> CORS --> CSRF --> JWT_FILTER
        JWT_FILTER --> TOKEN_EXTRACT --> CLAIMS_PARSE --> USER_LOAD --> SEC_CTX
    end

    subgraph Controller_Guard ["Authorization & Tenant Boundary Guard"]
        RBAC_CHECK{"@PreAuthorize Role Check"}
        TENANT_CHECK{"Multi-Tenant Boundary Check"}
        EXECUTE_CTRL["Target Controller Method Executed"]
        FORBIDDEN["HTTP 403 Forbidden"]
        
        SEC_CTX --> RBAC_CHECK
        RBAC_CHECK -- "Unauthorized Role" --> FORBIDDEN
        RBAC_CHECK -- "Authorized Role" --> TENANT_CHECK
        TENANT_CHECK -- "Cross-Institution Tampering" --> FORBIDDEN
        TENANT_CHECK -- "Valid Tenant / Global Admin" --> EXECUTE_CTRL
    end

    subgraph Multi_Tenancy_Model ["Multi-Tenant Data Scoping"]
        SUPER_ROLE["SUPER_ADMIN / ADMIN (Global Platform Access)"]
        COLLEGE_ROLE["COLLEGE_ADMIN / FACULTY (institution_id Scoped)"]
        STUDENT_ROLE["STUDENT (Own Profile & Scoped Problems)"]
        
        EXECUTE_CTRL --> SUPER_ROLE
        EXECUTE_CTRL --> COLLEGE_ROLE
        EXECUTE_CTRL --> STUDENT_ROLE
    end
```

---

### 3.3 Sandboxed Execution Engine (`CompilerService`)
The compiler engine executes untrusted polyglot user code securely without compromising the host machine:
1. **Dynamic File Generation**: Creates a randomized isolated folder in `app.compiler.temp-dir` (e.g. `/tmp/kartik_compiler/code_a1b2c3d4/`).
2. **Source Code Extraction**: Writes user code into proper source filenames (`Main.java`, `script.py`, `solution.cpp`, `main.c`, `app.js`).
3. **Subprocess Invocation (`ProcessBuilder`)**:
   * **Compilation Stage** (for compiled languages: Java, C, C++): Executes compiler binary (`javac`, `g++`, `gcc`) with diagnostic error interception.
   * **Execution Stage**: Spawns runtime with strict resource limits:
     * **Memory Ceiling**: `-Xmx128m` (Java) preventing memory exhaustion attacks.
     * **Timeout Watchdog**: 10-second hard asynchronous interrupt via `process.waitFor(timeout, TimeUnit.SECONDS)`. If exceeded, `process.destroyForcibly()` kills the PID and returns a `TIME_LIMIT_EXCEEDED` status.
     * **Output Truncation**: Standard output stream buffers are capped at 50,000 characters (`app.compiler.max-output`) to eliminate infinite-loop buffer overflow attacks.
4. **Cleanup Guarantee**: A `finally` block recursively deletes temporary source files and compiled binaries.

#### 📊 Subprocess Sandbox & Watchdog Lifecycle Diagram:
```mermaid
flowchart TD
    subgraph Compiler_Ingress ["Compiler Engine Ingress"]
        REQ["Execution Request (Language, Source Code, Stdin)"]
        TEMP_GEN["Temp Dir Allocator (/tmp/kartik_compiler/code_uuid/)"]
        SRC_GEN["Write Source Code (Main.java / solution.cpp / script.py)"]
        STDIN_GEN["Write Custom Input (stdin.txt)"]
        
        REQ --> TEMP_GEN --> SRC_GEN --> STDIN_GEN
    end

    subgraph Compilation_Stage ["Stage 1: Compilation (Java / C / C++)"]
        CHECK_COMPILED{"Requires Compilation?"}
        RUN_COMPILER["ProcessBuilder(javac / g++ / gcc)"]
        COMP_RESULT{"Exit Code == 0?"}
        COMP_ERR["Capture stderr -> Return COMPILATION_ERROR"]
        
        STDIN_GEN --> CHECK_COMPILED
        CHECK_COMPILED -- "Yes (Java, C, C++)" --> RUN_COMPILER --> COMP_RESULT
        COMP_RESULT -- "Non-Zero Exit" --> COMP_ERR
    end

    subgraph Runtime_Sandbox ["Stage 2: Sandboxed Runtime Subprocess"]
        SPAWN["ProcessBuilder: Spawn Isolated Subprocess"]
        MEM_LIMIT["Memory Ceiling (-Xmx128m on JVM)"]
        REDIRECT_IN["Redirect stdin from stdin.txt"]
        WATCHDOG["Async Watchdog Timer (10 Seconds Limit)"]
        
        CHECK_COMPILED -- "No (Python, JS)" --> SPAWN
        COMP_RESULT -- "Zero Exit (Success)" --> SPAWN
        SPAWN --> MEM_LIMIT
        SPAWN --> REDIRECT_IN
        SPAWN --> WATCHDOG
    end

    subgraph Execution_Monitoring ["Stage 3: Subprocess Monitoring & Safeguards"]
        TIMER{"Process finishes within 10s?"}
        KILL["Watchdog Triggers: process.destroyForcibly()"]
        TLE_RESP["Return TIME_LIMIT_EXCEEDED"]
        READ_STREAM["Capture stdout & stderr via BufferedReader"]
        BUFF_LIMIT{"Output Length > 50,000 chars?"}
        TRUNCATE["Truncate Buffer & Append Warning"]
        
        WATCHDOG --> TIMER
        TIMER -- "Timeout Exceeded" --> KILL --> TLE_RESP
        TIMER -- "Completed in Time" --> READ_STREAM --> BUFF_LIMIT
        BUFF_LIMIT -- "Yes" --> TRUNCATE
        BUFF_LIMIT -- "No" --> FINALIZE["Package Output, Latency (ms), Memory"]
        TRUNCATE --> FINALIZE
    end

    subgraph Disk_Cleanup ["Stage 4: Post-Execution Disk Cleanup"]
        FINALLY["Finally Block: FileSystemUtils.deleteRecursively()"]
        DELETE_DIR["Delete /tmp/kartik_compiler/code_uuid/"]
        RESP["Return JSON to Client with Points & Badges"]
        
        FINALIZE --> FINALLY
        COMP_ERR --> FINALLY
        TLE_RESP --> FINALLY
        FINALLY --> DELETE_DIR --> RESP
    end
```

---

### 3.4 AI Intelligence & Problem Generation Pipeline (`AIService` & `ProblemService`)
* **NVIDIA NIM Integration**: Connects to `https://integrate.api.nvidia.com/v1/chat/completions` using Bearer authentication.
* **Multi-Model Fallback Hierarchy**:
  1. `poolside/laguna-xs-2.1`: Ultra-low latency coding model returning direct, high-fidelity JSON.
  2. `minimaxai/minimax-m3.1-flash`: Fast backup LLM for high-throughput completions.
  3. `nvidia/nemotron-3.5-lightning-30b-a3b`: NVIDIA's advanced reasoning model.
* **Balanced-Brace JSON Parser (`cleanJson`)**: Robust scanner that filters markdown code fences (````json ... ````), skips reasoning tokens, tracks string escaping, and isolates the outer `{ ... }` object without failing on trailing commentary.
* **Dynamic Topic Synthesizer**: If the external AI service encounters network dropouts, `ProblemService` automatically switches to an in-memory algorithmic problem generator covering 15+ DSA domains (Trees, Graphs, DP, Stacks, Queues, Linked Lists, Matrices, Bitwise, Greedy) and generates custom problems for any user-defined topic string.

#### 📊 NVIDIA NIM AI Pipeline & Zero-Downtime Fallback Architecture:
```mermaid
flowchart TD
    subgraph Prompt_Construction ["AI Problem Prompt Builder"]
        USER_INPUT["Topic (e.g. 'Binary Search Tree'), Difficulty ('MEDIUM'), Lang ('Java')"]
        SYSTEM_PROMPT["System Instruction: Strict JSON Schema with Problem, Starter Code & Tests"]
        PROMPT_PKG["Packaged API Payload (temperature: 0.8, max_tokens: 2048)"]
        
        USER_INPUT --> SYSTEM_PROMPT --> PROMPT_PKG
    end

    subgraph Model_Failover_Pipeline ["NVIDIA NIM Multi-Model Failover"]
        M1["Primary Model: poolside/laguna-xs-2.1 (Low-Latency Coding LLM)"]
        M2["Secondary Model: minimaxai/minimax-m3.1-flash (High Throughput Backup)"]
        M3["Tertiary Model: nvidia/nemotron-3.5-lightning-30b-a3b (Reasoning Fallback)"]
        
        PROMPT_PKG --> M1
        M1 -- "HTTP 4xx / 5xx / Timeout" --> M2
        M2 -- "HTTP 4xx / 5xx / Timeout" --> M3
    end

    subgraph Sanitization_Engine ["Robust Balanced-Brace JSON Parser (cleanJson)"]
        RAW_TEXT["Raw LLM String Output"]
        STRIP_FENCE["Strip Markdown Code Fences (```json ... ```)"]
        SCANNER["Balanced-Brace Depth Counter: Tracks '{' and '}'"]
        REASON_FILTER["Skip 'thinking' and 'reasoning_content' tokens"]
        VALID_JSON["Extracted Clean Problem JSON"]
        
        M1 -- "Success" --> RAW_TEXT
        M2 -- "Success" --> RAW_TEXT
        M3 -- "Success" --> RAW_TEXT
        RAW_TEXT --> STRIP_FENCE --> SCANNER --> REASON_FILTER --> VALID_JSON
    end

    subgraph Offline_Synthesizer ["Zero-Downtime Fallback: DSA Topic Synthesizer"]
        OFFLINE_TRIGGER["All Cloud Endpoints Down / Offline"]
        TOPIC_MAPPER{"Topic Matches 15+ Known DSA Domains?"}
        DOMAIN_GEN["Domain Generator (Trees, Graphs, DP, Stacks, Queues, Matrices, etc.)"]
        DYNAMIC_SYNTH["Dynamic Algorithmic Topic Synthesizer"]
        
        M3 -- "All AI Models Failed" --> OFFLINE_TRIGGER --> TOPIC_MAPPER
        TOPIC_MAPPER -- "Matched Domain" --> DOMAIN_GEN --> VALID_JSON
        TOPIC_MAPPER -- "Custom / Unknown Topic" --> DYNAMIC_SYNTH --> VALID_JSON
    end

    subgraph Client_Delivery ["UI Delivery & Instant Assignment"]
        VALIDATE["Validate starterCode & sampleTestCases"]
        DELIVER["Deliver to College Admin / Student Modal"]
        
        VALID_JSON --> VALIDATE --> DELIVER
    end
```

---

### 3.5 Database & Entity-Relationship Schema

```mermaid
erDiagram
    INSTITUTIONS ||--o{ USERS : "enrolls"
    INSTITUTIONS ||--o{ PROBLEMS : "scopes"
    USERS ||--o{ EXECUTION_RECORDS : "runs"
    USERS ||--o{ PROBLEM_SUBMISSIONS : "submits"
    USERS ||--o{ ANTI_CHEAT_LOGS : "logs"
    PROBLEMS ||--o{ TEST_CASES : "contains"
    PROBLEMS ||--o{ PROBLEM_SUBMISSIONS : "evaluates"

    INSTITUTIONS {
        bigint id PK
        string name
        string license_key UK
        string status "PENDING, APPROVED, SUSPENDED"
        bigint super_admin_id
        timestamp created_at
    }

    USERS {
        bigint id PK
        string username UK
        string email UK
        string password
        string full_name
        string role "ADMIN, COLLEGE_ADMIN, FACULTY, STUDENT, USER"
        boolean is_active
        boolean is_disqualified
        int total_points
        int total_executions
        int successful_executions
        int cheat_violations
        bigint institution_id FK
        timestamp created_at
    }

    PROBLEMS {
        bigint id PK
        string title
        text description
        string difficulty "EASY, MEDIUM, HARD"
        int points
        string tags
        text sample_input
        text sample_output
        text starter_code
        bigint institution_id FK "NULL for Global"
        bigint created_by FK
    }

    TEST_CASES {
        bigint id PK
        bigint problem_id FK
        text input_data
        text expected_output
        boolean is_hidden
    }

    PROBLEM_SUBMISSIONS {
        bigint id PK
        bigint problem_id FK
        bigint user_id FK
        string language
        text code
        string status "ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED"
        int points_awarded
        timestamp submitted_at
    }

    ANTI_CHEAT_LOGS {
        bigint id PK
        bigint student_id FK
        string event_type "TAB_SWITCH, MULTI_FACE, PHONE_DETECTED, COPY_PASTE"
        int violation_count
        timestamp logged_at
    }

    EXECUTION_RECORDS {
        bigint id PK
        bigint user_id FK
        string language
        text code
        boolean success
        string status
        bigint execution_time_ms
        int points
        timestamp executed_at
    }
```

---

## 4. End-to-End System Workflows

---

### Workflow 1: College Registration, Verification & Role Onboarding

```mermaid
sequenceDiagram
    autonumber
    actor User as User / College Rep
    participant LoginUI as login.html (Frontend)
    participant AuthCtrl as AuthController
    participant AuthSvc as AuthService
    participant InstRepo as InstitutionRepository
    participant UserRepo as UserRepository
    actor SuperAdmin as Platform Super Admin
    participant AdminCtrl as AdminController

    User->>LoginUI: Fills Form: Username, Email, Password, College Name, Role (STUDENT / COLLEGE_ADMIN)
    LoginUI->>AuthCtrl: POST /api/auth/register { username, email, password, companyName, role }
    AuthCtrl->>AuthSvc: register(request)
    
    alt College Does Not Exist
        AuthSvc->>InstRepo: Create new Institution (Status = PENDING)
        AuthSvc->>UserRepo: Save User (Role = COLLEGE_ADMIN or STUDENT, isActive = false)
        AuthSvc->>InstRepo: Link super_admin_id = User.id (if COLLEGE_ADMIN)
        AuthSvc-->>LoginUI: 201 Created ("College registration submitted! PENDING approval.")
    else College Already Exists
        alt Role == COLLEGE_ADMIN
            AuthSvc->>UserRepo: Save User (Role = COLLEGE_ADMIN, isActive = based on College status)
        else Role == STUDENT
            AuthSvc->>UserRepo: Save User (Role = STUDENT, isActive = based on College status)
        end
        AuthSvc-->>LoginUI: 201 Created (Token issued or Pending notice)
    end

    Note over SuperAdmin, AdminCtrl: Admin Review Stage
    SuperAdmin->>AdminCtrl: GET /api/admin/colleges/pending
    AdminCtrl-->>SuperAdmin: Returns pending institutions with Admin Name & Email
    SuperAdmin->>AdminCtrl: POST /api/admin/colleges/{id}/approve
    AdminCtrl->>InstRepo: Set Status = APPROVED
    AdminCtrl->>UserRepo: Activate all enrolled students & college admins (isActive = true)
    AdminCtrl-->>SuperAdmin: 200 OK ("College approved successfully!")
```

---

### Workflow 2: Sandboxed Polyglot Code Execution Lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Coder / Student
    participant Monaco as Monaco Editor (compiler.html)
    participant Gateway as JwtAuthenticationFilter
    participant CompCtrl as CompilerController
    participant CompSvc as CompilerService
    participant Sandbox as Subprocess Sandbox (OS)
    participant DashSvc as DashboardService
    participant DB as MySQL Database

    Dev->>Monaco: Types Code + Custom Input -> Clicks 'Run Code' (Ctrl+Enter)
    Monaco->>Gateway: POST /api/compiler/run { language: 'java', code: '...', input: '...' }
    Gateway->>Gateway: Validate Bearer JWT token & extract userId / role
    Gateway->>CompCtrl: Forward authenticated request
    CompCtrl->>CompSvc: executeCode(request, currentUser)

    CompSvc->>CompSvc: Create temp directory (/tmp/kartik_compiler/job_uuid/)
    CompSvc->>CompSvc: Write source code to Main.java & input to stdin.txt

    alt Compiled Language (Java / C / C++)
        CompSvc->>Sandbox: Execute Compilation (javac Main.java)
        alt Compilation Fails
            Sandbox-->>CompSvc: stderr (Syntax errors)
            CompSvc-->>CompCtrl: Return COMPILATION_ERROR response
        end
    end

    CompSvc->>Sandbox: Execute Runtime (java -Xmx128m Main < stdin.txt)
    Note over Sandbox: Watchdog monitors CPU & 10s timeout
    
    alt Execution Successful
        Sandbox-->>CompSvc: stdout output captured within 10s
        CompSvc->>DashSvc: Calculate latency bonus & points (+10 base, +5 if <100ms)
        CompSvc->>DB: Save ExecutionRecord (code, latency, status, points)
        CompSvc->>DB: Update User stats (totalPoints, totalExecutions, successRate)
        CompSvc-->>CompCtrl: CompilerResponse (SUCCESS, output, timeMs, points)
    else Timeout Exceeded (> 10s)
        Sandbox->>Sandbox: process.destroyForcibly()
        CompSvc-->>CompCtrl: CompilerResponse (TIME_LIMIT_EXCEEDED)
    end

    CompSvc->>CompSvc: Finally: Clean up /tmp/kartik_compiler/job_uuid/
    CompCtrl-->>Monaco: JSON Execution Result
    Monaco-->>Dev: Renders output in terminal console with syntax colors & badges
```

---

### Workflow 3: AI Dynamic Problem Generation Lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor Admin as College Admin / Platform Admin
    participant UI as college.html (AI Problem Modal)
    participant ProbCtrl as ProblemController
    participant ProbSvc as ProblemService
    participant AISvc as AIService
    participant NVIDIA as NVIDIA NIM API (integrate.api.nvidia.com)

    Admin->>UI: Selects Topic ('Binary Search Tree'), Difficulty ('MEDIUM'), Language ('Java')
    UI->>ProbCtrl: POST /api/problems/ai-generate { topic, difficulty, language }
    ProbCtrl->>ProbCtrl: Verify User is ADMIN, SUPER_ADMIN, or COLLEGE_ADMIN
    ProbCtrl->>ProbSvc: generateAiProblem(request)

    ProbSvc->>ProbSvc: Construct structured prompt with formatted sections & JSON schema
    ProbSvc->>AISvc: callNvidiaAI(prompt)

    AISvc->>NVIDIA: POST /v1/chat/completions (model: 'poolside/laguna-xs-2.1', max_tokens: 2048)
    
    alt NVIDIA NIM Responds
        NVIDIA-->>AISvc: Returns JSON completion with Problem Statement & Rules
        AISvc-->>ProbSvc: Raw text response
        ProbSvc->>ProbSvc: cleanJson() -> Balanced-Brace Scanner isolates valid JSON
    else NVIDIA Unavailable or Network Error
        AISvc-->>ProbSvc: Fallback Triggered
        ProbSvc->>ProbSvc: Topic-Accurate DSA Synthesizer (Generates unique BST problem)
    end

    ProbSvc->>ProbSvc: Validate and inject starterCode (public class Main) & Sample Test Case
    ProbSvc-->>ProbCtrl: Map containing title, description, testCases, starterCode
    ProbCtrl-->>UI: 200 OK (Problem JSON)
    UI-->>Admin: Auto-populates 'Assign Problem' modal for review and instant publish
```

---

### Workflow 4: Automated Problem Submission & Multi-Test Grading

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student Coder
    participant UI as compiler.html (Assessment View)
    participant ProbCtrl as ProblemController
    participant ProbSvc as ProblemService
    participant CompSvc as CompilerService
    participant DB as MySQL Database

    Student->>UI: Solves Problem -> Clicks 'Submit Final Solution'
    UI->>ProbCtrl: POST /api/problems/{id}/submit { language, code }
    ProbCtrl->>ProbSvc: submitSolution(problemId, request, currentUser)

    ProbSvc->>DB: Fetch Problem & TestCases (Public sample + Hidden verification cases)
    
    loop For Every Test Case
        ProbSvc->>CompSvc: executeCodeWithInput(code, language, tc.inputData)
        CompSvc-->>ProbSvc: actualOutput, status, executionTimeMs
        ProbSvc->>ProbSvc: Compare actualOutput.trim() == tc.expectedOutput.trim()
        alt Output Mismatch
            ProbSvc-->>Student: Return 'WRONG_ANSWER' with failed test case details
        end
    end

    Note over ProbSvc: All Test Cases Passed Successfully
    ProbSvc->>DB: Record ProblemSubmission (Status = ACCEPTED, points = problem.points)
    ProbSvc->>DB: Increment User.totalPoints (+50) & recalculate Tier Ranking
    ProbSvc-->>ProbCtrl: SubmissionResponse (ACCEPTED, 100% tests passed, pointsAwarded)
    ProbCtrl-->>UI: Displays Success Banner & Gold Points Animation
```

---

### Workflow 5: Real-Time AI Proctoring & Anti-Cheat Surveillance

```mermaid
sequenceDiagram
    autonumber
    actor Candidate as Student in Secure Exam Mode
    participant CamMic as Camera & Microphone
    participant TF as TensorFlow.js & COCO-SSD (Browser GPU)
    participant WebAudio as Web Audio API Analyser
    participant ProctorCtrl as ProctorController
    participant AntiCheatSvc as AntiCheatService
    participant DB as MySQL Database
    actor Invigilator as College Admin / Proctor

    Note over Candidate, TF: Local Real-Time Edge Inference (Every 1000ms)
    CamMic->>TF: Video Stream Frames (WebGL Tensor)
    CamMic->>WebAudio: Real-time Audio Frequency Data

    alt Infraction Detected (Multi-Person, Cell Phone, Missing Face, High Voice)
        TF->>TF: Flag violation (e.g., 'cell phone' detected with 88% confidence)
        TF->>Candidate: Trigger Audio Buzzer & Red Alert Banner in UI
        TF->>ProctorCtrl: POST /api/proctor/violation { eventType: 'PHONE_DETECTED', details: '...' }
        ProctorCtrl->>AntiCheatSvc: recordViolation(studentId, event)
        AntiCheatSvc->>DB: Insert AntiCheatLog & increment User.cheatViolations
        
        alt Violations Exceed Threshold (>= 5)
            AntiCheatSvc->>DB: Set User.isDisqualified = true, User.isActive = false
            AntiCheatSvc-->>Candidate: Session Auto-Cut: Exam Terminated
        end
    end

    Note over Invigilator, ProctorCtrl: Live Proctor Surveillance Feed
    Invigilator->>ProctorCtrl: GET /api/proctor/feed (or college.html surveillance tab)
    ProctorCtrl->>DB: Query active candidate streams & violation counts
    ProctorCtrl-->>Invigilator: Live camera snapshots, status badges & infraction logs
```

---

## 5. Security & Isolation Threat Model

| Threat Vector | Attack Scenario | KartikTerminal Mitigation Architecture |
| :--- | :--- | :--- |
| **Infinite Loop / Fork Bomb** | User executes `while(true)` or `fork()` bomb | Subprocess executed with **10-second hard watchdog**. Unresponsive processes are forcibly terminated with `process.destroyForcibly()`. |
| **Memory Exhaustion (DoS)** | Program attempts to allocate huge memory heap | Java sub-runtime strictly constrained by JVM argument `-Xmx128m`. OS enforces process memory ceiling. |
| **Output Flooding Attack** | Infinite printing loop (`while(true) print('A')`) | Output stream buffer reader enforces hard cap of `50,000` characters (`app.compiler.max-output`). Excess stream is truncated. |
| **Cross-Tenant Inspection** | College Admin accesses records of another college | Database queries for students and problems are multi-tenant scoped (`institution_id`). Security checks verify caller institution ID against target entity. |
| **Exam Integrity Violations** | Student uses mobile phone, switches tabs, copies code | Client-side **TensorFlow.js COCO-SSD** flags devices and tab switches; clipboard events tracked; infractions recorded in `anti_cheat_logs`; auto-cut termination upon 5 violations. |
| **Privilege Escalation** | Student invokes admin endpoints (`/api/admin/**`) | Spring Security method security `@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'COLLEGE_ADMIN')")` validates role directly from cryptographic JWT claims. |

---

## 6. Technology Stack Summary

* **Backend Framework**: Spring Boot 3.2.x (Java 17)
* **Security & Auth**: Spring Security 6.x, JJWT (HMAC-SHA256), Google OAuth2 Client
* **Data Layer**: Hibernate 6.x, Spring Data JPA, MySQL 8.0 (InnoDB), HikariCP
* **Code Editor**: Microsoft Monaco Editor (VS Code Engine)
* **Computer Vision AI**: TensorFlow.js, COCO-SSD MobileNet v2 Model
* **LLM Engine**: NVIDIA NIM Cloud API (`poolside/laguna-xs-2.1`, `nvidia/nemotron-3.5-lightning-30b-a3b`, `minimaxai/minimax-m3.1-flash`)
* **Audio & Voice Processing**: Web Audio API (`AnalyserNode`), Web Speech API (SpeechRecognition & SpeechSynthesis)
* **Build System**: Apache Maven 3.8+ with Maven Wrapper (`mvnw`)

