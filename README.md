<div align="center">

# 🖥️ KartikTerminal
### **Enterprise Online Code Compiler Platform**

*A secure, full-stack online code execution platform powered by Spring Boot, MySQL, Google OAuth2, JWT Authentication, and a modern web interface.*

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring_Security-Authentication-success?style=for-the-badge&logo=springsecurity)
![JWT](https://img.shields.io/badge/JWT-Secure_Login-black?style=for-the-badge&logo=jsonwebtokens)
![Google OAuth](https://img.shields.io/badge/Google-OAuth2-4285F4?style=for-the-badge&logo=google)
![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven)

---

### 💻 Compile • Execute • Compete • Learn

</div>

---

# 📖 Overview

**KartikTerminal** is a secure, full-stack **Online Code Compiler & Coding Platform** that enables users to write, compile, execute, and analyze code directly from the browser.

The platform features **Google OAuth2 authentication**, **JWT security**, **real-time code execution**, **leaderboards**, **coding statistics**, and a personalized dashboard. Every code execution is securely logged, scored, and ranked, making it suitable for coding practice, online assessments, and competitive programming environments.

Built with **Spring Boot**, **Spring Security**, **MySQL**, **JWT**, and a lightweight frontend, the application follows enterprise-grade backend architecture and security best practices.

---

# ✨ Core Features

---

# 🔐 Authentication & Security

Secure authentication powered by Spring Security.

### Features

- Google OAuth2 Login
- Email & Password Login
- JWT Authentication
- BCrypt Password Encryption
- Role-Based Authorization
- Secure Session Management

---

# 💻 Online Code Compiler

Execute code directly from your browser.

### Features

- Online Code Editor
- Multi-Language Support
- Instant Code Execution
- Execution History
- Runtime Analysis
- Output Viewer
- Error Console

---

# 📊 User Dashboard

Track your coding progress.

Dashboard includes

- Total Executions
- Coding History
- Performance Statistics
- Earned Points
- Current Rank
- Recent Activities
- Language Usage
- Profile Information

---

# 🏆 Leaderboard

Compete with other programmers.

Features

- Global Ranking
- Top Coders
- Personal Rank
- Points System
- Tier System
- Coding Performance

---

# 👨‍💼 Admin Panel

Administrative features include

- User Management
- Role Management
- Platform Statistics
- User Moderation
- System Monitoring

---

# 📈 Code Execution Analytics

Every execution records

- Programming Language
- Execution Time
- Status
- Output
- Runtime Errors
- Timestamp
- Earned Points

---

# 🛡 Secure Code Execution

Security-first execution environment.

Features

- Isolated Sandbox
- Temporary Workspace
- Execution Timeout
- Memory Limitation
- Output Limitation
- Input Validation

---

# 🎯 Supported Languages

- ☕ Java
- 🐍 Python
- 🌐 JavaScript
- ⚙ C
- ⚙ C++
- 🦫 Go

---

# 🏅 Gamification

## Points System

| Action | Points |
|----------|---------|
| Successful Execution | +10 |
| Runtime < 100 ms | +5 Bonus |
| Runtime < 500 ms | +3 Bonus |
| Runtime < 1000 ms | +1 Bonus |
| Java / Go | +2 Bonus |
| C / C++ | +3 Bonus |
| Failed Execution | 0 |

---

## User Tiers

| Tier | Points |
|-------|---------|
| 🥉 Bronze | 0 – 99 |
| 🥈 Silver | 100 – 249 |
| 🥇 Gold | 250 – 499 |
| 💎 Platinum | 500 – 999 |
| 💠 Diamond | 1000+ |

---

# 🛠 Technology Stack

| Category | Technology |
|-----------|------------|
| Backend | Spring Boot 3.x |
| Language | Java 17 |
| Security | Spring Security |
| Authentication | JWT + Google OAuth2 |
| Database | MySQL |
| ORM | Spring Data JPA / Hibernate |
| Build Tool | Maven |
| Frontend | HTML5 |
| Styling | CSS3 |
| Programming | JavaScript |

---

# 📂 Project Structure

```text
kartikterminal-backend/
│
├── src/
│
├── main/
│   ├── java/
│   │
│   └── com/
│       └── kartik/
│           └── terminal/
│
│               ├── config/
│               ├── controller/
│               ├── dto/
│               ├── entity/
│               ├── exception/
│               ├── repository/
│               ├── security/
│               ├── service/
│               └── KartikTerminalApplication.java
│
├── resources/
│   ├── static/
│   │   ├── login.html
│   │   ├── compiler.html
│   │   ├── dashboard.html
│   │   ├── leaderboard.html
│   │   └── auth.js
│   │
│   └── application.properties
│
├── pom.xml
└── README.md
```

---

# 🚀 Getting Started

## Clone Repository

```bash
git clone https://github.com/yourusername/kartikterminal-backend.git
```

---

# 💾 Database Setup

Create database

```sql
CREATE DATABASE kartikterminal;
```

Configure

```properties
spring.datasource.username=kartik

spring.datasource.password=your_password
```

---

# 🔐 Google OAuth2 Setup

Configure your Google credentials.

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID

spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
```

Redirect URI

```
http://localhost:8080/login/oauth2/code/google
```

---

# ▶ Run Application

Build

```bash
mvn clean install -DskipTests
```

Run

```bash
mvn spring-boot:run
```

Application starts at

```
http://localhost:8080
```

---

# 🌐 Application Pages

| Page | URL |
|------|-----|
| Login | `/login.html` |
| Compiler | `/compiler.html` |
| Dashboard | `/dashboard.html` |
| Leaderboard | `/leaderboard.html` |

---

# 🔌 REST API

## Authentication

| Method | Endpoint |
|----------|------------------------------|
| POST | `/api/auth/register` |
| POST | `/api/auth/login` |
| GET | `/api/auth/me` |
| PUT | `/api/auth/profile` |
| POST | `/api/auth/change-password` |
| POST | `/api/auth/logout` |

---

## OAuth2

| Method | Endpoint |
|----------|-------------------------------|
| GET | `/oauth2/authorization/google` |
| GET | `/login/oauth2/code/google` |

---

## Compiler

| Method | Endpoint |
|----------|----------------------------|
| POST | `/compiler/run` |
| POST | `/api/compiler/run` |
| GET | `/api/compiler/languages` |

---

## Dashboard

| Method | Endpoint |
|----------|-----------------------------|
| GET | `/api/dashboard` |
| GET | `/api/dashboard/stats` |
| GET | `/api/dashboard/history` |

---

## Leaderboard

| Method | Endpoint |
|----------|-----------------------------|
| GET | `/api/leaderboard` |

---

## Admin

| Method | Endpoint |
|----------|------------------------------|
| GET | `/api/admin/users` |
| GET | `/api/admin/stats` |
| PUT | `/api/admin/users/{id}/role` |
| DELETE | `/api/admin/users/{id}` |

---

# 🔒 Security Features

- JWT Authentication
- Google OAuth2 Login
- BCrypt Password Hashing
- Secure REST APIs
- Role-Based Authorization
- Code Sandbox
- Execution Timeout
- Memory Limit
- Output Limit
- CORS Protection
- Input Validation
- Automatic JWT Validation

---

# 🚀 Future Enhancements

Upcoming features

- 🤖 AI Code Review
- 💬 Collaborative Coding
- 👥 Coding Rooms
- 📹 Live Coding Interviews
- 📂 Project Management
- 📝 Coding Challenges
- 🏅 Weekly Contests
- ☁ Docker-Based Sandboxing
- 📊 Advanced Analytics
- 📱 Mobile Application
- 🌍 Multi-Language UI
- 🔥 VS Code Theme Support
- 🧠 AI Debugging Assistant

---

# 🎯 Learning Outcomes

This project demonstrates

- Spring Boot Development
- Spring Security
- JWT Authentication
- Google OAuth2 Integration
- REST API Development
- MySQL Integration
- Hibernate ORM
- Role-Based Access Control
- Secure Backend Development
- Online Code Execution
- Leaderboard System
- Enterprise Project Architecture

---

# 🤝 Contributing

Contributions are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Push your branch.
5. Open a Pull Request.

---

# 📜 License

Licensed under the **MIT License**.

---

<div align="center">

## ⭐ Support This Project

If you found this project useful, consider giving it a **⭐ Star** on GitHub.

Your support helps improve the project and motivates future development.

---

# 🖥️ KartikTerminal

### **Code. Compile. Compete. Conquer.**

Built with ❤️ using **Spring Boot • Java • Spring Security • JWT • Google OAuth2 • MySQL**

</div>
