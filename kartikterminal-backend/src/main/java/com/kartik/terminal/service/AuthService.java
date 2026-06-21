package com.kartik.terminal.service;

import com.kartik.terminal.dto.AuthDTOs.*;
import com.kartik.terminal.entity.User;
import com.kartik.terminal.repository.UserRepository;
import com.kartik.terminal.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // ── Register (email/password fallback, optional) ──
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            throw new RuntimeException("Username '" + request.getUsername() + "' is already taken!");
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("Email '" + request.getEmail() + "' is already registered!");

        User user = User.builder()
                .username(request.getUsername().toLowerCase().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName() != null ? request.getFullName().trim() : request.getUsername())
                .role(User.Role.USER)
                .isActive(true)
                .totalExecutions(0).successfulExecutions(0)
                .totalPoints(0).totalExecutionTimeMs(0L)
                .favoriteLanguage("java")
                .build();

        User saved = userRepository.save(user);
        String token = jwtTokenProvider.generateTokenWithClaims(
                saved.getUsername(),
                Map.of("role", saved.getRole().name(), "userId", saved.getId()));
        return AuthResponse.success(token, saved.getUsername(), saved.getEmail(),
                saved.getFullName(), saved.getRole().name(), saved.getId());
    }

    // ── Login (email/password) ──
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(auth);

            User user = userRepository.findByUsername(request.getUsernameOrEmail())
                    .orElseGet(() -> userRepository.findByEmail(request.getUsernameOrEmail())
                            .orElseThrow(() -> new RuntimeException("User not found")));

            userRepository.updateLastLogin(user.getId(), LocalDateTime.now());
            String token = jwtTokenProvider.generateTokenWithClaims(
                    user.getUsername(),
                    Map.of("role", user.getRole().name(), "userId", user.getId()));
            return AuthResponse.success(token, user.getUsername(), user.getEmail(),
                    user.getFullName(), user.getRole().name(), user.getId());
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid username/email or password!");
        }
    }

    // ── Get currently authenticated user ──
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    // ── Check username availability ──
    public boolean isUsernameTaken(String username) {
        return userRepository.existsByUsername(username.toLowerCase().trim());
    }

    // ── Change password ──
    @Transactional
    public String changePassword(ChangePasswordRequest req) {
        User user = getCurrentUser();
        if (user.getPassword().isEmpty())
            throw new RuntimeException("OAuth2 accounts cannot use password login.");
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new RuntimeException("Current password is incorrect!");
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
        return "Password changed successfully!";
    }

    // ── Update profile ──
    @Transactional
    public User updateProfile(UpdateProfileRequest req) {
        User user = getCurrentUser();
        if (req.getFullName() != null && !req.getFullName().isBlank())
            user.setFullName(req.getFullName().trim());
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            if (!req.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(req.getEmail()))
                throw new RuntimeException("Email already in use!");
            user.setEmail(req.getEmail().toLowerCase().trim());
        }
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        return userRepository.save(user);
    }
}
