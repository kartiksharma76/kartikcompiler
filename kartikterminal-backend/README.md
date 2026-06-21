# 🖥️ KartikTerminal — Full Stack Backend

**Spring Boot + MySQL + Google OAuth2 + JWT**  
Online code compiler with login, dashboard, and leaderboard.

---

## 📁 Project Structure

```
kartikterminal-backend/
│
├── pom.xml                                         ← Maven dependencies
│
├── src/main/resources/
│   ├── application.properties                      ← DB + OAuth2 + JWT config
│   └── static/
│       ├── auth.js                                 ← JWT manager (include in all pages)
│       ├── login.html                              ← Google login + email/password
│       ├── compiler.html                           ← Main editor (your existing file)
│       ├── dashboard.html                          ← User stats + history
│       └── leaderboard.html                        ← Global rankings
│
└── src/main/java/com/kartik/terminal/
    ├── KartikTerminalApplication.java
    │
    ├── entity/
    │   ├── User.java                               ← Users table
    │   └── ExecutionRecord.java                    ← Every code run stored here
    │
    ├── repository/
    │   ├── UserRepository.java
    │   └── ExecutionRecordRepository.java
    │
    ├── security/
    │   ├── JwtTokenProvider.java                   ← Generate + validate JWT
    │   ├── JwtAuthenticationFilter.java            ← Reads Bearer token from headers
    │   ├── CustomUserDetailsService.java           ← Loads user from DB
    │   ├── OAuth2SuccessHandler.java               ← Runs after Google login → issues JWT
    │   └── OAuth2FailureHandler.java
    │
    ├── config/
    │   ├── SecurityConfig.java                     ← Route permissions + OAuth2 setup
    │   └── WebMvcConfig.java                       ← Static file serving
    │
    ├── dto/
    │   ├── AuthDTOs.java                           ← Register/Login request+response
    │   └── CompilerDTOs.java                       ← Execution + Dashboard + Leaderboard
    │
    ├── service/
    │   ├── AuthService.java                        ← Register, Login, Profile
    │   ├── CompilerService.java                    ← Runs code in subprocess sandbox
    │   └── DashboardService.java                   ← Stats, history, leaderboard data
    │
    ├── controller/
    │   ├── AuthController.java                     ← /api/auth/**
    │   ├── CompilerController.java                 ← /compiler/run
    │   ├── DashboardController.java                ← /api/dashboard/**
    │   ├── LeaderboardController.java              ← /api/leaderboard
    │   └── AdminController.java                    ← /api/admin/**
    │
    └── exception/
        └── GlobalExceptionHandler.java
```

---

## ⚙️ Step 1 — Prerequisites

Install these on your machine:

| Tool | Version | Download |
|------|---------|----------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| MySQL | 8.0+ | https://dev.mysql.com/downloads |
| Node.js | 18+ | https://nodejs.org (for JS execution) |
| Python | 3.10+ | https://python.org (for Python execution) |
| GCC/G++ | any | `sudo apt install gcc g++` (Linux) |
| Go | 1.21+ | https://go.dev (optional) |

---

## 🗄️ Step 2 — MySQL Setup

```sql
-- Run in MySQL Workbench or terminal
CREATE DATABASE kartikterminal
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'kartik'@'localhost' IDENTIFIED BY 'strongpassword123';
GRANT ALL PRIVILEGES ON kartikterminal.* TO 'kartik'@'localhost';
FLUSH PRIVILEGES;
```

Then update `application.properties`:
```properties
spring.datasource.username=kartik
spring.datasource.password=strongpassword123
```

> Tables are **auto-created** by Hibernate on first run (`ddl-auto=update`).

---

## 🔐 Step 3 — Google OAuth2 Setup

1. Go to **https://console.cloud.google.com**
2. Create a new project (e.g. "KartikTerminal")
3. Navigate to **APIs & Services → Credentials**
4. Click **Create Credentials → OAuth 2.0 Client ID**
5. Application type: **Web application**
6. Add Authorized Redirect URI:
   ```
   http://localhost:8080/login/oauth2/code/google
   ```
7. Copy the **Client ID** and **Client Secret**
8. Paste into `application.properties`:
   ```properties
   spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID.apps.googleusercontent.com
   spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
   ```

---

## 🚀 Step 4 — Run the Application

```bash
# Clone / open the project folder
cd kartikterminal-backend

# Build
mvn clean install -DskipTests

# Run
mvn spring-boot:run
```

Server starts at: **http://localhost:8080**

---

## 🌐 Step 5 — Open in Browser

| URL | Page |
|-----|------|
| http://localhost:8080/login.html | Login page |
| http://localhost:8080/compiler.html | Code editor |
| http://localhost:8080/dashboard.html | Your stats |
| http://localhost:8080/leaderboard.html | Global rankings |

---

## 🔗 API Endpoints Reference

### Auth — `/api/auth/`
```
POST   /api/auth/register          Register with email+password
POST   /api/auth/login             Login with email+password → returns JWT
GET    /api/auth/me                Get current user info (requires JWT)
PUT    /api/auth/profile           Update name / email / avatar
POST   /api/auth/change-password   Change password
GET    /api/auth/check-username/{u} Check if username is available
POST   /api/auth/logout            Logout (logs the event)
```

### OAuth2 — Google Login
```
GET    /oauth2/authorization/google    Redirects to Google
GET    /login/oauth2/code/google       Google callback (handled by Spring)
                                       → Issues JWT → redirects to compiler.html?token=...
```

### Compiler
```
POST   /compiler/run               Run code (used by compiler.html)
POST   /api/compiler/run           Run code → full JSON response
GET    /api/compiler/languages     List supported languages
```

### Dashboard
```
GET    /api/dashboard              Full dashboard (stats + history + charts)
GET    /api/dashboard/stats        Stats only
GET    /api/dashboard/history      Paginated execution history
                                   ?page=0&size=20
```

### Leaderboard
```
GET    /api/leaderboard            Top 50 coders + your rank
```

### Admin (requires ADMIN role)
```
GET    /api/admin/users            All users
GET    /api/admin/stats            Platform stats
PUT    /api/admin/users/{id}/role  Change user role
DELETE /api/admin/users/{id}       Deactivate user
```

---

## 🔑 How JWT Works in This App

```
1. User clicks "Login with Google"
        ↓
2. Google authenticates → redirects to /login/oauth2/code/google
        ↓
3. OAuth2SuccessHandler runs:
   - Finds or creates user in MySQL
   - Generates JWT token (24hr expiry)
   - Redirects to: /compiler.html?token=eyJ...
        ↓
4. auth.js on compiler.html:
   - Reads ?token= from URL
   - Saves to localStorage as 'kt_token'
   - Cleans URL (no token visible in address bar)
   - Patches window.fetch() to auto-add Authorization header
        ↓
5. Every API call (runCode, dashboard, etc):
   Headers: { Authorization: "Bearer eyJ..." }
        ↓
6. JwtAuthenticationFilter validates token → sets SecurityContext
```

---

## 🏆 Points System

| Action | Points |
|--------|--------|
| Successful execution | +10 base |
| Time < 100ms | +5 bonus |
| Time < 500ms | +3 bonus |
| Time < 1000ms | +1 bonus |
| C / C++ language | +3 language bonus |
| Java / Go language | +2 language bonus |
| Failed execution | +0 |

### Tiers
| Tier | Points Required |
|------|----------------|
| 🥉 BRONZE | 0 – 99 |
| 🥈 SILVER | 100 – 249 |
| 🥇 GOLD | 250 – 499 |
| 💎 PLATINUM | 500 – 999 |
| 💠 DIAMOND | 1000+ |

---

## 🛡️ Security Features

- **Google OAuth2** — no passwords stored for OAuth users
- **BCrypt (strength 12)** — for email/password accounts
- **JWT (HS256, 24hr)** — stateless authentication
- **Code sandbox** — each execution runs in isolated temp directory
- **Execution timeout** — 10 seconds max per run
- **Memory limit** — JVM `-Xmx128m` for Java runs
- **Output limit** — 50KB max output
- **CORS configured** — only allowed origins accepted
- **Input validation** — `@Valid` on all request bodies
- **Tab switch detection** — auto-resets code if security mode ON

---

## 🐛 Common Errors & Fixes

### `Failed to determine a suitable driver class`
→ Add MySQL password in `application.properties`

### `OAuth2: redirect_uri_mismatch`
→ In Google Console, add exactly: `http://localhost:8080/login/oauth2/code/google`

### `javac: command not found`
→ Install JDK (not just JRE), add to PATH

### `401 Unauthorized` on `/compiler/run`
→ Token expired — user needs to login again. `auth.js` handles this automatically.

### Port 8080 already in use
→ `server.port=9090` in application.properties

---

## 🔧 Making Yourself Admin

After first login, run this SQL:
```sql
UPDATE users
SET role = 'ADMIN'
WHERE username = 'your_username';
```

---

## 📦 Build JAR for Deployment

```bash
mvn clean package -DskipTests

# JAR is at:
target/terminal-1.0.0.jar

# Run on server:
java -jar target/terminal-1.0.0.jar \
  --spring.datasource.password=PROD_PASSWORD \
  --spring.security.oauth2.client.registration.google.client-id=PROD_CLIENT_ID \
  --spring.security.oauth2.client.registration.google.client-secret=PROD_SECRET
```

For production, also update redirect URI in Google Console to your domain:
```
https://yourdomain.com/login/oauth2/code/google
```
And in `application.properties`:
```properties
app.oauth2.success-redirect-url=https://yourdomain.com/compiler.html
app.cors.allowed-origins=https://yourdomain.com
```
