package com.kartik.terminal.repository;

import com.kartik.terminal.entity.AiInterview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiInterviewRepository extends JpaRepository<AiInterview, Long> {
    List<AiInterview> findByStudentId(Long studentId);
}
