package com.kartik.terminal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resume_experiences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Experience {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String role;

    @Column
    private String startDate;

    @Column
    private String endDate;

    @Column(columnDefinition = "TEXT")
    private String description;
}
