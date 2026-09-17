# 🖥️ KartikTerminal: Enterprise Online Code Compiler Platform
## 📊 High-Density Executive Presentation Deck (9-Slide Master Format)

---

## 📌 SLIDE 1: Title & Executive Summary
### [Project Overview & Core Identity]

#### 🏷️ Header Information
* **Project Name:** **KartikTerminal — Enterprise Online Code Compiler & Assessment Platform**
* **Project Type:** Full-Stack Cloud-Native Web IDE & Real-Time Code Execution Engine
* **Team Leader:** Kartikey Sharma
* **Team Members:** Gulshan Kumar, Bimlesh Pandey, Basudev Kumar
* **Department / College:** Department of Computer Science and Information Technology, Sagar Institute of Research & Technology (SIRT), Bhopal
* **Academic Session:** 2025 – 2026 | **Mentor:** [Professor / Mentor Name]

#### 🚀 Executive Summary & Value Proposition
* **What is KartikTerminal?** A centralized, browser-native compilation and execution ecosystem that allows users to write, run, debug, and benchmark multi-language code in real time without any local environment configuration.
* **Core Technological Foundation:** Java 17 + Spring Boot 3.x, Spring Security with JWT & Google OAuth2, MySQL RDBMS, Monaco Code Editor, and Isolated Subprocess Sandboxing.
* **Target Audience:** Engineering Students, Competitive Programmers, Academic Evaluators, and Technical Recruiters.

```
+---------------------------------------------------------------------------------------------------+
| 🌟 KEY PLATFORM PILLARS:                                                                          |
| [1] Zero-Setup Cloud IDE   [2] 6+ Major Languages   [3] Subprocess Sandboxing   [4] Gamified Stats |
+---------------------------------------------------------------------------------------------------+
```

---

## 📌 SLIDE 2: Problem Statement & Project Objectives
### [Need Analysis & Solution Mapping]

#### ⚠️ Industry & Academic Pain Points (The Problem)
1. **Heavy Local Environment Overhead:** Installing heavy IDEs (VS Code, IntelliJ, Eclipse, GCC, JDK, Python) requires 10GB+ storage and high RAM, slowing down low-spec machines.
2. **"It Works On My Machine" Inconsistency:** Discrepancies across OS environments (Windows vs. macOS vs. Linux path variables and compiler flags) cause deployment and evaluation failures.
3. **Beginner Friction & Setup Fatigue:** Novice programmers waste significant time resolving environment variable errors instead of developing core problem-solving algorithms.
4. **Lack of Centralized Assessment Tools:** Academic labs and interviewers lack isolated, lightweight environments with built-in test-case runners and anti-cheat tracking.

#### 🎯 Strategic Project Objectives (The Solution)
* **Instant Browser-Based Execution:** Deliver sub-second code compilation with syntax highlighting, line numbering, and custom `stdin` support directly from web browsers.
* **Multi-Language Parity:** Complete support for **Java, Python 3, C, C++, JavaScript (Node.js), and Go**.
* **Isolated Security Sandbox:** Prevent malicious system calls, infinite loops, and fork bombs using a strict 5000ms timeout watchdog and memory caps.
* **Progress Tracking & Analytics:** Persist execution history, time metrics, output streams, problem-solving points, and global leaderboard rankings.

---

## 📌 SLIDE 3: End-to-End System Architecture
### [Multi-Tier Distributed Design & Execution Pipeline]

#### 🏗️ High-Level System Architecture Diagram
```
====================================================================================================
 🌐 1. CLIENT LAYER (Browser UI)
    • Monaco / Ace Code Editor | Syntax Highlighting | Custom Input (stdin) | Output & Error Console
====================================================================================================
                                      │  HTTPS / REST APIs / JSON Payloads
                                      ▼
====================================================================================================
 ⚙️ 2. APPLICATION & SECURITY LAYER (Spring Boot 3.x)
    • Spring Security (OAuth2 + JWT Filter) ──► Request Validation & Payload Sanitization
    • Controller Layer (/api/compiler, /api/auth) ──► Service Orchestrator ──► Analytics Tracker
====================================================================================================
                                      │
                 ┌────────────────────┴────────────────────┐
                 ▼                                         ▼
=======================================   ==========================================================
 🛡️ 3. SANDBOXED EXECUTION ENGINE         💾 4. PERSISTENCE & ANALYTICS LAYER (MySQL)
    • Temporary Workspace Generator        • User Credentials & Roles (Admin/User)
    • ProcessBuilder Execution Workers     • Historical Execution Records (Time, Memory, Status)
    • 5000ms Timeout Watchdog Guard        • Problem Sets, Automated Test Cases & Quizzes
    • stdout / stderr Stream Interceptor   • Gamified Leaderboard & Point Engine
=======================================   ==========================================================
```

#### 🔄 Technical Execution Flow:
1. User writes code & provides optional `stdin` input -> Hits **Run** button.
2. Spring Boot filters validate JWT identity & sanitize source code.
3. Execution worker creates an isolated temp directory (`/temp/exec_<uuid>/Solution.ext`).
4. Native compiler/runtime is invoked via `ProcessBuilder` with strict time limit (5000ms).
5. Output streams (`stdout` & `stderr`) are captured into thread-safe buffers.
6. Execution metrics (runtime in ms, memory, exit code) are logged into MySQL, and JSON response is delivered back to UI.

---

## 📌 SLIDE 4: Technology Stack & Implementation Ecosystem
### [5-Pillar Industrial Grade Tech Matrix]

| Layer / Domain | Technologies & Libraries | Key Role & Implementation Purpose |
| :--- | :--- | :--- |
| **Frontend UI** | HTML5, CSS3, JavaScript (ES6+), Monaco/Ace Editor | Provides responsive dark-themed IDE canvas, syntax highlighting, and dynamic console views. |
| **Backend Core** | Java 17 LTS, Spring Boot 3.x, Spring Data JPA | Handles high-throughput REST APIs, asynchronous execution pipelines, and transactional services. |
| **Security & Auth** | Spring Security 6, JWT (RFC 7519), Google OAuth2, BCrypt | Token-based stateless authentication, password hashing, and role-based access control (RBAC). |
| **Execution Engine** | Java `ProcessBuilder`, OpenJDK, GCC/G++, Python 3, Node.js, Go | Local subprocess management, compiler flag tuning, isolated temp workspaces, and timeout watchdogs. |
| **Persistence & DB** | MySQL 8.0, Hibernate ORM | Relational storage for users, execution records, coding problems, submissions, and rankings. |
| **DevOps & Build** | Maven, Docker, Git/GitHub, Render Cloud Hosting | Continuous Integration, reproducible builds, and containerized cloud deployment. |

---

## 📌 SLIDE 5: Core Functional Modules
### [Comprehensive Feature Breakdown]

```
+───────────────────────────────────+───────────────────────────────────+
| 🔐 1. Authentication & Roles      | 💻 2. Multi-Language Compiler     |
| • Google OAuth2 & Email/Password  | • Java, Python, C, C++, JS, Go    |
| • Stateless JWT Token Management  | • Custom stdin input injection    |
| • Role-Based Control (User/Admin) | • Starter code templates & themes |
+───────────────────────────────────+───────────────────────────────────+
| 🛡️ 3. Sandboxing & Stream Capture | 📊 4. Analytics & Gamification   |
| • 5-Second Timeout Auto-Kill      | • Execution runtime (ms) tracking |
| • Memory and Output Buffer limits | • Success rate & score calculation|
| • Distinct stdout & stderr views  | • Real-time global leaderboard    |
+───────────────────────────────────+───────────────────────────────────+
| 📝 5. Assessment & Problems       | 🤖 6. AI & Anti-Cheat Subsystem   |
| • Problem library with test cases | • Tab-switch and copy-paste logs  |
| • Multiple Hidden Test Evaluation | • Code similarity checks          |
| • Timed Quizzes & Exam Engine     | • AI-assisted performance report  |
+───────────────────────────────────+───────────────────────────────────+
```

---

## 📌 SLIDE 6: Database Design & Relational Schema
### [Entity-Relationship Structure & Persistence Strategy]

```
                       ┌───────────────────────┐
                       │        USERS          │
                       ├───────────────────────┤
                       │ PK  id                │
                       │     name              │
                       │     email (UNIQUE)    │
                       │     password_hash     │
                       │     role (USER/ADMIN) │
                       │     points            │
                       └───────────┬───────────┘
                                   │ 1
                                   │
              ┌────────────────────┼────────────────────┐
              │ N                  │ N                  │ N
              ▼                    ▼                    ▼
   ┌──────────────────────┐ ┌───────────────┐ ┌──────────────────────┐
   │   EXECUTION_RECORD   │ │  QUIZ_SUBMIT  │ │  PROBLEM_SUBMISSION  │
   ├──────────────────────┤ ├───────────────┤ ├──────────────────────┤
   │ PK  id               │ │ PK  id        │ │ PK  id               │
   │ FK  user_id          │ │ FK  user_id   │ │ FK  user_id          │
   │     language         │ │ FK  quiz_id   │ │ FK  problem_id (FK)──┼──┐
   │     code_snippet     │ │     score     │ │     status (AC/WA)   │  │
   │     status           │ │     submitted │ │     execution_time   │  │
   │     execution_time   │ └───────────────┘ └──────────────────────┘  │
   │     output_text      │                                             │
   └──────────────────────┘                                             │
                                                                        │ N
                                   ┌──────────────────────┐             │
                                   │       PROBLEM        │             │
                                   ├──────────────────────┤             │
                                   │ PK  id               │◄────────────┘
                                   │     title            │
                                   │     difficulty       │
                                   │     time_limit       │
                                   └───────────┬──────────┘
                                               │ 1
                                               │ N
                                               ▼
                                   ┌──────────────────────┐
                                   │      TEST_CASE       │
                                   ├──────────────────────┤
                                   │ PK  id               │
                                   │ FK  problem_id       │
                                   │     input            │
                                   │     expected_output  │
                                   │     is_hidden (BOOL) │
                                   └──────────────────────┘
```

---

## 📌 SLIDE 7: Rigorous Testing & Security Edge Cases
### [Comprehensive QA Verification Matrix]

| Test Scenario | Test Input Code / Payload | Expected System Behavior | Actual Result & Status |
| :--- | :--- | :--- | :--- |
| **Standard Execution** | `System.out.println("Hello World");` (Java) | Output: `Hello World`, Exit code `0` | ✅ **PASS** (Runtime: ~120ms) |
| **Syntax Error** | `int a = 10` (Missing semicolon in C++) | Error captured from `stderr`, line number highlighted | ✅ **PASS** (Compilation Error caught) |
| **Runtime Exception** | `print(10 / 0)` (Python zero division) | Graceful ZeroDivisionError response, server stays stable | ✅ **PASS** (Zero crash on server) |
| **Infinite Loop / TLE** | `while(true){ i++; }` | Watchdog timer triggers at 5000ms -> Kills child PID | ✅ **PASS** (Returns: Time Limit Exceeded) |
| **Large Output Attack** | Infinite print loop (`while(1) printf("A");`) | Output stream truncated at 100KB buffer limit | ✅ **PASS** (Memory leak prevented) |
| **Security Injection** | `Runtime.getRuntime().exec("shutdown");` | Sandboxed process permissions block destructive OS calls | ✅ **PASS** (Security Access Denied) |
| **Custom stdin Support** | `cin >> a >> b; cout << a+b;` with input `5 7` | Correct standard input piped to process; Output `12` | ✅ **PASS** (Input successfully parsed) |

---

## 📌 SLIDE 8: Comparative Analysis & Key Advantages
### [Why KartikTerminal Outperforms Existing Solutions]

#### 📊 Feature Matrix Comparison
| Evaluation Metric | Local Desktop IDEs | Generic Online Tools | KartikTerminal Platform |
| :--- | :--- | :--- | :--- |
| **Setup & Onboarding** | High (Install JDK, GCC, Path) | Instant | **Instant (Zero Installation)** |
| **Hardware Footprint** | Heavy (Needs 4GB+ RAM) | Low | **Minimal (Any Browser/Mobile)** |
| **Multi-Language Agility** | Requires separate toolchains | Limited | **6 Core Languages Out-of-the-Box** |
| **Security & Sandboxing** | Runs at Host privilege level | Often basic | **Isolated Subprocess & 5s Auto-Kill** |
| **Data Persistence** | Local files only | Session-only / No history | **MySQL JWT History & Analytics** |
| **Evaluation Readiness** | Manual checking | Code execution only | **Automated Test Cases & Quizzes** |

#### 🌟 Primary System Benefits:
* ⚡ **100% Platform Independent:** Runs on Windows, macOS, Linux, ChromeOS, iPad, and Android.
* 📈 **Continuous Progress Analytics:** Tracks user mastery per language with score increments.
* 🏢 **Institutional Utility:** Built-in support for online coding tests, lab exams, and automated scoring.

---

## 📌 SLIDE 9: Limitations, Future Roadmap & Conclusion
### [Project Horizon & Final Takeaways]

#### ⚠️ Current System Limitations:
* **Internet Prerequisite:** Requires active cloud connectivity to execute backend jobs.
* **Compute Bounds:** Heavy GUI development (Java Swing, Tkinter) and GPU/ML training are restricted by design.

#### 🚀 Future Scope & Enhancements:
1. **🤖 Integrated AI Code Mentor:** Integrated LLM (Gemini/OpenAI) for intelligent syntax diagnosis, auto-completion, and code optimization suggestions.
2. **👥 Collaborative Pair Programming:** Real-time multi-user simultaneous code editor via WebSockets & WebRTC.
3. **📦 Multi-File & Package Support:** Support for multi-file projects, custom Maven `pom.xml`, and Python `pip` dependencies.
4. **🏆 Automated Hackathon & Contest Engine:** Live time-bounded coding contests with real-time dynamic scoreboards.

#### 🎯 Conclusion:
* **Outcome:** Delivered a production-ready, full-stack online compiler engineered with robust backend security, sub-second execution speeds, and modern developer UI.
* **Impact:** Drastically minimizes setup barriers for beginners, provides scalable grading for educators, and serves as a reliable cloud IDE platform.

---

### 📚 References & Repository
* **Spring Boot Documentation:** [https://spring.io/projects/spring-boot](https://spring.io/projects/spring-boot)
* **Spring Security & JWT RFC 7519:** [https://jwt.io/introduction](https://jwt.io/introduction)
* **Monaco Editor Engine:** [https://microsoft.github.io/monaco-editor/](https://microsoft.github.io/monaco-editor/)
* **Project GitHub Repository:** `https://github.com/kartiksharma76/kartikcompiler`
