package com.kartik.terminal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_record_id", nullable = false)
    private ExecutionRecord executionRecord;

    @Column(nullable = false)
    private Integer syntaxScore;

    @Column(nullable = false)
    private Integer logicalScore;

    @Column(columnDefinition = "TEXT")
    private String securityVulnerabilities;

    @Column(length = 50)
    private String timeComplexity;

    @Column(length = 50)
    private String spaceComplexity;

    @Column(columnDefinition = "JSON")
    private String refactorSuggestions;

    @Column(nullable = false)
    private Integer overallAiScore;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime generatedAt;
}
