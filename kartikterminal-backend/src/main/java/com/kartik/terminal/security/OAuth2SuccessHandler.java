package com.kartik.terminal.security;

import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Called automatically by Spring after successful Google OAuth2 login.
 *
 * Flow:
 *   User clicks "Login with Google"
 *   → Google redirects back with authorization code
 *   → Spring exchanges code for user info
 *   → This handler runs
 *   → We create/find the user in MySQL
 *   → Generate a JWT token
 *   → Redirect to compiler.html?token=JWT
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${app.oauth2.success-redirect-url}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract Google profile attributes
        String email      = oAuth2User.getAttribute("email");
        String name       = oAuth2User.getAttribute("name");
        String pictureUrl = oAuth2User.getAttribute("picture");
        String googleSub  = oAuth2User.getAttribute("sub");   // unique Google user ID

        log.info("OAuth2 login: email={}", email);

        // Find or create user in our DB
        User user = findOrCreateUser(email, name, pictureUrl, googleSub);

        // Update last login time
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        // Issue JWT
        String jwt = jwtTokenProvider.generateTokenWithClaims(
                user.getUsername(),
                Map.of(
                    "role",   user.getRole().name(),
                    "userId", user.getId(),
                    "email",  user.getEmail()
                )
        );

        // Determine request base URL to avoid redirecting to hardcoded host/port
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);
        if (("http".equals(scheme) && serverPort != 80) || ("https".equals(scheme) && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }
        
        String targetUrl = successRedirectUrl.startsWith("http") ? successRedirectUrl : baseUrl.toString() + successRedirectUrl;

        // Redirect to frontend with token as query param
        // Frontend JS reads ?token=... and stores it in localStorage
        String redirectUrl = UriComponentsBuilder
                .fromUriString(targetUrl)
                .queryParam("token", jwt)
                .queryParam("username", user.getUsername())
                .build().toUriString();

        log.info("Redirecting user '{}' after OAuth2 login to: {}", user.getUsername(), targetUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // -------------------------------------------------------
    // Find existing user by email, or auto-register them
    // -------------------------------------------------------
    private User findOrCreateUser(String email, String name,
                                  String pictureUrl, String googleSub) {

        String cleanEmail = email != null ? email.toLowerCase().trim() : "";
        boolean isMasterAdmin = "kartiksharma768976@gmail.com".equalsIgnoreCase(cleanEmail);

        Optional<User> existingUser = userRepository.findByEmail(cleanEmail);

        if (existingUser.isPresent()) {
            // User already registered — update their avatar in case it changed & ensure admin privileges if master admin
            User user = existingUser.get();
            boolean changed = false;

            if (isMasterAdmin && (user.getRole() != User.Role.ADMIN || Boolean.FALSE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsDisqualified()))) {
                user.setRole(User.Role.ADMIN);
                user.setIsActive(true);
                user.setIsDisqualified(false);
                changed = true;
                log.info("Master admin logged in via OAuth2. Re-applied ADMIN role & active status.");
            }

            if (pictureUrl != null && !pictureUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(pictureUrl);
                changed = true;
            }
            if (changed) {
                userRepository.save(user);
            }
            return user;
        }

        // First time login — auto-register
        String username = isMasterAdmin ? "kartik_admin" : generateUsername(cleanEmail, name);

        User newUser = User.builder()
                .username(username)
                .email(cleanEmail)
                .password("")                    // No password — OAuth2 only account
                .fullName(name != null ? name.trim() : (isMasterAdmin ? "Kartik Admin" : username))
                .avatarUrl(pictureUrl)
                .role(isMasterAdmin ? User.Role.ADMIN : User.Role.USER)
                .isActive(true)
                .isDisqualified(false)
                .totalExecutions(0)
                .successfulExecutions(0)
                .totalPoints(0)
                .totalExecutionTimeMs(0L)
                .favoriteLanguage("java")
                .build();

        User saved = userRepository.save(newUser);
        log.info("Auto-registered new OAuth2 user: {} with role {}", saved.getUsername(), saved.getRole());
        return saved;
    }

    // -------------------------------------------------------
    // Generate a clean username from email or Google name
    // Ensures uniqueness by appending a number if needed
    // -------------------------------------------------------
    private String generateUsername(String email, String name) {
        // Try name first (e.g. "Kartik Sharma" → "kartik_sharma")
        String base = (name != null && !name.isBlank())
                ? name.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "")
                : email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9_]", "_");

        // Ensure max 20 chars
        base = base.length() > 16 ? base.substring(0, 16) : base;
        if (base.isBlank()) base = "user";

        // Check uniqueness
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
