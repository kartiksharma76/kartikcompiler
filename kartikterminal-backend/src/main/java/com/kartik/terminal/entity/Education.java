package com.kartik.terminal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resume_educations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(nullable = false)
    private String institution;

    @Column(nullable = false)
    private String degree;

    @Column
    private String fieldOfStudy;

    @Column
    private String startDate;

    @Column
    private String endDate;

    @Column
    private String grade;
}
