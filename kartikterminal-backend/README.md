# 🚀 KartikTerminal — Next-Gen Cloud IDE, AI Sandbox & Proctoring Platform

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2+-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![TensorFlow.js](https://img.shields.io/badge/TensorFlow.js-4.17+-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/js)
[![NVIDIA NIM](https://img.shields.io/badge/NVIDIA%20NIM-AI%20Llama%203.3-76B900?style=for-the-badge&logo=nvidia&logoColor=white)](https://build.nvidia.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

<p align="center">
  <strong>An enterprise-grade, polyglot cloud compiler, automated coding assessment engine, and AI-driven secure proctoring ecosystem.</strong>
</p>

[Key Features](#-key-features) •
[AI Proctoring](#-ai-computer-vision--audio-proctoring) •
[Architecture](#-system-architecture) •
[Getting Started](#-getting-started) •
[API Reference](#-api-endpoints-reference) •
[Deployment](#-docker--production-deployment)

</div>

---

## 🌟 Overview

**KartikTerminal** is a cloud-based development environment and assessment system designed for universities, coding bootcamps, and recruitment drives. It combines high-speed multi-language sandboxed code execution, real-time AI-assisted problem generation, deep anti-cheat analytics, and automated computer vision proctoring in a VS Code-inspired browser console.

---

## ✨ Key Features

### 1. ⚡ High-Performance Polyglot Execution Sandbox
- **12+ Supported Languages & Runtimes**: Java, Python 3, C++, C, Node.js / JavaScript, TypeScript, Go, Rust, PHP, Ruby, Bash, and MySQL.
- **Isolated Process Sandboxing**: Subprocess virtualization with memory boundaries (`-Xmx128m`), aggressive CPU throttling, and strict execution timeouts (10s max).
- **Interactive Stdin/Stdout & Terminal Emulation**: Integrated bottom terminal with draggable resizing, ANSI color parsing, tab switching, and custom shell toggles.
- **Monaco Code Editor**: VS Code core editor with code folding, autocomplete, bracket colorization, minimap, formatting shortcuts, and multiple themes (*Dracula, Monokai, Nord, Solarized, GitHub*).

### 2. 🛡️ AI Computer Vision & Audio Proctoring (Secure Mode)
- **TensorFlow.js & COCO-SSD Deep Learning**: Real-time on-device video stream inference.
- **Multi-Face Surveillance**: Flags whenever more than one person enters camera view.
- **Absence & Look-Away Detection**: Detects missing face or sustained gaze distraction (>3.5s).
- **Object Detection (Anti-Cheat)**: Detects prohibited devices including mobile phones, secondary laptops, tablets, and physical reference books.
- **Audio & Speech Analysis**: Web Audio API `AnalyserNode` monitoring real-time acoustic levels, triggering alerts for continuous conversation or whispered speech.
- **Real-Time Live Proctor Dashboard**: Live WebRTC / JPEG stream broadcasting (`/proctor.html`) for exam invigilators with active candidate grid and infraction timelines.
- **Auto-Cut Termination**: Automated assessment disqualification and session freezing upon reaching maximum violation limits.

### 3. 🧠 NVIDIA NIM & Dynamic AI Problem Generator
- **Multi-Model LLM Pipeline**: Powered by NVIDIA NIM (`meta/llama-3.3-70b-instruct`, `meta/llama-3.1-8b-instruct`) with dynamic entropy temperature adjustments.
- **Instant DSA Challenge Generation**: Produces complete problem statements with constraints, difficulty ratings, sample inputs/outputs, starter code, and **2 Hidden Test Cases**.
- **Automated Sandbox Grading**: Evaluates submitted student code against public sample cases and sandboxed hidden test suites in parallel.

### 4. 🎓 College ERP & Academic Portal
- **Institution Multi-Tenancy**: Manage university departments, student batches, programming courses, and scheduled exams.
- **Custom Exam Workflows**: Create targeted coding assessments with secure proctoring configurations, time bounds, and real-time live monitoring feeds.

### 5. 🏆 Gamification & Competitive Leaderboards
- **Dynamic Points & Rating Engine**: Base point awards modified by execution latency (<100ms bonus), algorithmic complexity, and language tier bonuses.
- **Ranking Tiers**: 🥉 Bronze → 🥈 Silver → 🥇 Gold → 💎 Platinum → 💠 Diamond.
- **Global & College Leaderboards**: Filter rankings by global community or college department.

---

## 👁️ AI Computer Vision & Audio Proctoring

```
 ┌─────────────────────────────────────────────────────────────┐
 │                 Browser Camera & Mic Stream                 │
 └──────────────────────────────┬──────────────────────────────┘
                                │
          ┌─────────────────────┴─────────────────────┐
          ▼                                           ▼
┌───────────────────────────┐               ┌──────────────────┐
│   TensorFlow.js COCO-SSD  │               │  Web Audio API   │
│   Computer Vision Engine  │               │  Acoustic Energy │
└─────────┬─────────────────┘               └─────────┬────────┘
          │                                           │
  ┌───────┴──────────────────────┐            ┌───────┴────────┐
  ▼                              ▼            ▼                ▼
[Multi-Person > 1]     [Phone / Books]    [Voice Speech]   [Looking Away]
  │                              │            │                │
  └──────────────────────────────┼────────────┴────────────────┘
                                 ▼
                 ┌───────────────────────────────┐
                 │    Proctor Violation Engine   │
                 │  - Red Alert Warning Banner   │
                 │  - Audio Buzzer Notification  │
                 │  - REST Frame Streaming       │
                 │  - Auto-Cut Disqualification  │
                 └───────────────────────────────┘
```

---

## 🏗️ System Architecture

```
kartikterminal-backend/
├── src/main/java/com/kartik/terminal/
│   ├── config/                     # SecurityConfig, WebMvcConfig, Async & CORS configs
│   ├── controller/                 # REST APIs (Auth, Compiler, Proctor, College, AI, Dashboard)
│   ├── dto/                        # Strongly typed Request/Response Transfer Objects
│   ├── entity/                     # JPA Entities (User, ExecutionRecord, Problem, College, TestCase)
│   ├── exception/                  # Centralized GlobalExceptionHandler
│   ├── repository/                 # Spring Data JPA Repositories
│   ├── security/                   # Spring Security 6, JWT Token Provider, Google OAuth2 Handlers
│   └── service/                    # Core Business Logic & Process Execution Sandboxes
│       ├── AIService.java          # NVIDIA NIM LLM Integration & Fallback Router
│       ├── AuthService.java        # User Registration, Authentication & JWT Lifecycle
│       ├── CompilerService.java    # Subprocess Virtualization & Multi-Language Runner
│       ├── DashboardService.java   # Analytics, User Metrics & Performance Graphs
│       └── ProblemService.java     # Automated Grader & 2 Hidden Test Case Evaluator
│
└── src/main/resources/
    ├── application.properties      # Central DB, JWT, OAuth2 & AI Keys
    └── static/                     # Frontend SPA Client Suite
        ├── compiler.html           # Monaco IDE & Live Proctoring Workspace
        ├── dashboard.html          # User Analytics & History
        ├── proctor.html            # Real-Time Invigilator Surveillance Console
        ├── college.html            # College & Academic Management Portal
        ├── leaderboard.html        # Global & Institutional Rankings
        ├── chat.html               # AI Coding Assistant & Complexity Profiler
        ├── login.html              # Modern Glassmorphic Login & OAuth2 Portal
        ├── auth.js                 # Unified JWT Session Interceptor
        └── responsive.css          # Responsive Mobile & Desktop Layout Tokens
```

---

## 🛠️ Prerequisites

Before launching KartikTerminal, ensure you have the following installed:

- **Java JDK**: 17 or 21+ ([Eclipse Adoptium](https://adoptium.net))
- **Maven**: 3.8+ (or use the included `mvnw` wrapper)
- **MySQL Server**: 8.0+ ([MySQL Downloads](https://dev.mysql.com/downloads/mysql/))
- **Language Compilers / Interpreters** (for languages you wish to execute locally):
  - `python` (Python 3.10+)
  - `node` & `npm` (Node.js 18+)
  - `g++` / `gcc` (MinGW-w64 on Windows or `build-essential` on Linux)
  - `go` (Go 1.21+)
  - `rustc` (Rust 1.70+)

---

## 🚀 Quick Start Guide

### 1. Database Configuration
Open MySQL command line or Workbench and initialize the database:

```sql
CREATE DATABASE kartikterminal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'kartik'@'localhost' IDENTIFIED BY 'strongpassword123';
GRANT ALL PRIVILEGES ON kartikterminal.* TO 'kartik'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Configure Environment Properties
Edit `src/main/resources/application.properties` with your database credentials and API keys:

```properties
# Database Connection
spring.datasource.url=jdbc:mysql://localhost:3306/kartikterminal?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=kartik
spring.datasource.password=strongpassword123

# JWT Configuration
app.jwt.secret=9a78f6e5d4c3b2a10987654321fedcba0123456789abcdef0123456789abcdef
app.jwt.expiration-ms=86400000

# Google OAuth2 (Optional for Social Login)
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET

# NVIDIA NIM AI Integration (For Problem Generation)
nvidia.nim.api-key=nvapi-YOUR_NVIDIA_API_KEY
```

### 3. Build & Run Application

Using Maven Wrapper:
```bash
# Build project
./mvnw clean compile

# Run Spring Boot server
./mvnw spring-boot:run
```

Using standard Maven:
```bash
mvn clean package -DskipTests
java -jar target/terminal-1.0.0.jar
```

Server will start on **`http://localhost:8080`**.

---

## 🌐 Application Pages & Routes

| Web Page | Route | Description |
|:---|:---|:---|
| **Login & Register** | `/login.html` | Google OAuth2 and JWT email/password authentication |
| **Monaco Cloud IDE** | `/compiler.html` | Code editor with terminal, problem statement drawer, & AI proctoring |
| **User Dashboard** | `/dashboard.html` | Code submission history, ranking tiers, and execution charts |
| **Proctor Monitor** | `/proctor.html` | Real-time live video feeds & anti-cheat infraction timeline |
| **College ERP** | `/college.html` | Batch assignment, custom assessments, and departmental analytics |
| **Leaderboard** | `/leaderboard.html` | Global & College coding leaderboards |
| **AI Assistant** | `/chat.html` | Interactive AI assistant for debugging and optimization |

---

## 📡 Key API Endpoints

### 🔐 Authentication & Profile (`/api/auth`)
- `POST /api/auth/register` — Register a new account
- `POST /api/auth/login` — Login and receive JWT access token
- `GET /api/auth/me` — Retrieve profile & role permissions
- `PUT /api/auth/profile` — Update user profile details

### 💻 Code Compilation & Execution (`/compiler`, `/api/compiler`)
- `POST /compiler/run` — Run code in isolated sandboxed subprocess
- `POST /api/compiler/run` — Run code with structured JSON metrics
- `GET /api/compiler/languages` — List supported compiler environments

### 📹 Live Proctoring Surveillance (`/api/proctor`)
- `POST /api/proctor/stream-frame` — Transmit real-time camera snapshot & violation state
- `GET /api/proctor/active-feeds` — Invigilator endpoint to fetch active student streams
- `POST /api/proctor/stop-feed/{userId}` — Gracefully terminate student proctoring stream

### 🧩 Problems & Automated 2 Hidden Test Cases Grader (`/api/problems`)
- `GET /api/problems` — Fetch list of available coding problems
- `GET /api/problems/{id}` — Fetch problem details with sample I/O
- `POST /api/problems/generate-ai` — Generate instant DSA problem using NVIDIA Llama
- `POST /api/problems/{id}/submit` — Sandboxed grading against 1 sample + 2 hidden test cases

### 📊 Dashboard & Anti-Cheat Analytics (`/api/dashboard`)
- `GET /api/dashboard/stats` — Overall statistics (submissions, points, infractions)
- `GET /api/dashboard/history` — Paginated code run history
- `POST /api/dashboard/log-cheat` — Log anti-cheat infraction to database

---

## 🐳 Docker & Production Deployment

### Building with Docker
A multi-stage `Dockerfile` is included for zero-configuration containerization:

```bash
# Build Docker image
docker build -t kartikterminal:latest .

# Run container
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/kartikterminal?useSSL=false" \
  -e SPRING_DATASOURCE_USERNAME="kartik" \
  -e SPRING_DATASOURCE_PASSWORD="strongpassword123" \
  --name kartikterminal-app \
  kartikterminal:latest
```

---

## 🛡️ Security & Sandboxing Policies

- **Process Isolation**: Every code execution spawns in a transient, isolated directory cleaned up on process exit.
- **Resource Limits**: Strict CPU execution timeouts prevent infinite loops; memory ceilings protect host resources.
- **Role-Based Access Control**: Sensitive administrative endpoints (`/api/admin/**`, `/proctor.html`) are guarded by Spring Security with `ADMIN` and `COLLEGE_ADMIN` authorities.
- **Tamper-Proof Anti-Cheat Engine**: Prevents copy-pasting unauthorized code, tracks window defocusing, and terminates fullscreen violations.

---

## 🤝 Contributing

Contributions, feature requests, and issue reports are welcome!
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.

<div align="center">
  <sub>Engineered with ❤️ by <strong>Kartik Sharma</strong></sub>
</div>
