package com.kartik.terminal.repository;

import com.kartik.terminal.entity.ExecutionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExecutionRecordRepository extends JpaRepository<ExecutionRecord, Long> {

    // User's execution history (paginated)
    Page<ExecutionRecord> findByUserIdOrderByExecutedAtDesc(Long userId, Pageable pageable);

    // All user executions (no pagination)
    List<ExecutionRecord> findByUserIdOrderByExecutedAtDesc(Long userId);

    // Count by language for a user
    @Query("SELECT e.language, COUNT(e) FROM ExecutionRecord e WHERE e.user.id = :userId GROUP BY e.language")
    List<Object[]> countByLanguageForUser(@Param("userId") Long userId);

    // Successful runs count
    long countByUserIdAndSuccessTrue(Long userId);

    // Total runs today
    @Query("SELECT COUNT(e) FROM ExecutionRecord e WHERE e.user.id = :userId AND e.executedAt >= :today")
    long countTodayExecutions(@Param("userId") Long userId, @Param("today") LocalDateTime today);

    // Recent executions for all users (admin dashboard)
    @Query("SELECT e FROM ExecutionRecord e ORDER BY e.executedAt DESC")
    Page<ExecutionRecord> findAllRecentExecutions(Pageable pageable);

    // User's best execution time per language
    @Query("SELECT e.language, MIN(e.executionTimeMs) FROM ExecutionRecord e " +
           "WHERE e.user.id = :userId AND e.success = true GROUP BY e.language")
    List<Object[]> findBestTimePerLanguage(@Param("userId") Long userId);

    // Total platform executions today
    @Query("SELECT COUNT(e) FROM ExecutionRecord e WHERE e.executedAt >= :today")
    long countPlatformExecutionsToday(@Param("today") LocalDateTime today);

    // Global language usage stats
    @Query("SELECT e.language, COUNT(e) FROM ExecutionRecord e GROUP BY e.language ORDER BY COUNT(e) DESC")
    List<Object[]> getGlobalLanguageStats();

    // User streak - days with at least one execution
    @Query("SELECT DISTINCT DATE(e.executedAt) FROM ExecutionRecord e WHERE e.user.id = :userId ORDER BY DATE(e.executedAt) DESC")
    List<Object[]> getExecutionDates(@Param("userId") Long userId);
}
