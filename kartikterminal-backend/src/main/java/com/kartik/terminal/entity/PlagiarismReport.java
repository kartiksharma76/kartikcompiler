package com.kartik.terminal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "plagiarism_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlagiarismReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_1_id", nullable = false)
    private ProblemSubmission submission1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_2_id", nullable = false)
    private ProblemSubmission submission2;

    @Column(nullable = false)
    private Double similarityPercentage;

    @Column(columnDefinition = "JSON")
    private String matchedLines;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFlagged = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime generatedAt;
}
