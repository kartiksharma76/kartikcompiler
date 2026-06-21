package com.kartik.terminal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_interviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 150)
    private String vivaTopic;

    @Column(columnDefinition = "JSON")
    private String aiTranscript;

    @Column(nullable = false)
    @Builder.Default
    private Integer confidenceScore = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer technicalAccuracyScore = 0;

    @Column(columnDefinition = "TEXT")
    private String overallFeedback;

    @Column
    private String audioRecordingUrl;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
