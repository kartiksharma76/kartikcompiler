package com.kartik.terminal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "executionHistory"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDisqualified = false;

    @Column
    private String avatarUrl;

    // Leaderboard stats
    @Column(nullable = false)
    @Builder.Default
    private Integer totalExecutions = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer successfulExecutions = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer cheatViolations = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalPoints = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer quizPoints = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer aiPoints = 0;

    @Column(nullable = false)
    @Builder.Default
    private Long totalExecutionTimeMs = 0L;

    @Column(nullable = false)
    @Builder.Default
    private String favoriteLanguage = "java";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLoginAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "student_id_number", length = 50)
    private String studentIdNumber;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExecutionRecord> executionHistory;

    public enum Role {
        USER, ADMIN, MODERATOR, SUPER_ADMIN, COLLEGE_ADMIN, FACULTY, STUDENT
    }

    // Helper method
    public double getSuccessRate() {
        if (totalExecutions == 0) return 0.0;
        return (double) successfulExecutions / totalExecutions * 100;
    }

    public double getAverageExecutionTime() {
        if (successfulExecutions == 0) return 0.0;
        return (double) totalExecutionTimeMs / successfulExecutions;
    }
}
