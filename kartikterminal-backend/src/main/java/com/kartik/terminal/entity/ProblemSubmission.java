package com.kartik.terminal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "problem_submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Column(nullable = false)
    private Boolean isSolved;

    @Column(nullable = false)
    private Integer testCasesPassed;

    @Column(nullable = false)
    private Integer totalTestCases;

    @Column(nullable = false)
    private Long executionTimeMs;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime submittedAt;
}
