package com.kartik.terminal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String language;

    @Column(columnDefinition = "LONGTEXT")
    private String code;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "LONGTEXT")
    private String output;

    @Column(columnDefinition = "TEXT")
    private String errorOutput;

    @Column(nullable = false)
    private Boolean success;

    @Column(nullable = false)
    private Long executionTimeMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.SUCCESS;

    @Column(length = 100)
    private String title;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime executedAt;

    public enum ExecutionStatus {
        SUCCESS, COMPILE_ERROR, RUNTIME_ERROR, TIMEOUT, MEMORY_LIMIT
    }

    // Points calculation logic
    public static int calculatePoints(boolean success, long execTimeMs, String language) {
        if (!success) return 0;
        int base = 10;
        // Faster execution = more points
        if (execTimeMs < 100) base += 5;
        else if (execTimeMs < 500) base += 3;
        else if (execTimeMs < 1000) base += 1;
        // Language difficulty bonus
        return switch (language.toLowerCase()) {
            case "cpp", "c" -> base + 3;
            case "java", "go" -> base + 2;
            default -> base;
        };
    }
}
