package com.kartik.terminal.repository;

import com.kartik.terminal.entity.AntiCheatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AntiCheatLogRepository extends JpaRepository<AntiCheatLog, Long> {
    List<AntiCheatLog> findByExamId(Long examId);
    List<AntiCheatLog> findByStudentIdAndExamId(Long studentId, Long examId);
}
