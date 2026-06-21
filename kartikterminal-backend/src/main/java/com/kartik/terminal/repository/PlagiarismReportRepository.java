package com.kartik.terminal.repository;

import com.kartik.terminal.entity.PlagiarismReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlagiarismReportRepository extends JpaRepository<PlagiarismReport, Long> {
    List<PlagiarismReport> findByExamId(Long examId);
}
