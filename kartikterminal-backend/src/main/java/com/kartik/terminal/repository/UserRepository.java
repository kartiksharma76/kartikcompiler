package com.kartik.terminal.repository;

import com.kartik.terminal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Top users by points (Leaderboard)
    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.totalPoints DESC")
    List<User> findTopUsersByPoints();

    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.quizPoints DESC")
    List<User> findTopUsersByQuizPoints();

    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.aiPoints DESC")
    List<User> findTopUsersByAiPoints();

    // Top users by total executions
    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.totalExecutions DESC")
    List<User> findTopUsersByExecutions();

    // Users active in last 7 days
    @Query("SELECT u FROM User u WHERE u.lastLoginAt >= :since AND u.isActive = true")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);

    // Update last login
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLoginAt = :time WHERE u.id = :id")
    void updateLastLogin(@Param("id") Long id, @Param("time") LocalDateTime time);

    // Update stats after execution
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.totalExecutions = u.totalExecutions + 1, " +
           "u.successfulExecutions = u.successfulExecutions + :success, " +
           "u.totalPoints = u.totalPoints + :points, " +
           "u.totalExecutionTimeMs = u.totalExecutionTimeMs + :execTime, " +
           "u.favoriteLanguage = :lang " +
           "WHERE u.id = :id")
    void updateExecutionStats(@Param("id") Long id,
                              @Param("success") int success,
                              @Param("points") int points,
                              @Param("execTime") long execTime,
                              @Param("lang") String lang);

    // Count total active users
    long countByIsActiveTrue();
}
