package com.kartik.terminal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resume_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(nullable = false)
    private String title;

    @Column
    private String projectUrl;

    @Column(columnDefinition = "TEXT")
    private String description;
}
