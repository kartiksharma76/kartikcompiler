package com.kartik.terminal.repository;

import com.kartik.terminal.entity.AiAnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiAnalysisReportRepository extends JpaRepository<AiAnalysisReport, Long> {
    Optional<AiAnalysisReport> findByExecutionRecordId(Long executionRecordId);
}
