package com.kartik.terminal.repository;

import com.kartik.terminal.entity.Institution;
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

    // Company-specific queries
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.institution = :inst ORDER BY u.totalPoints DESC")
    List<User> findTopUsersByPointsAndInstitution(@Param("inst") Institution inst);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.institution = :inst ORDER BY u.quizPoints DESC")
    List<User> findTopUsersByQuizPointsAndInstitution(@Param("inst") Institution inst);

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.institution = :inst ORDER BY u.aiPoints DESC")
    List<User> findTopUsersByAiPointsAndInstitution(@Param("inst") Institution inst);

    long countByInstitutionAndIsActiveTrue(Institution institution);

    // Global queries (Excluding users who have an institution/company)
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.institution IS NULL ORDER BY u.totalPoints DESC")
    List<User> findTopUsersByPointsExcludingInstitutions();

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.institution IS NULL ORDER BY u.quizPoints DESC")
    List<User> findTopUsersByQuizPointsExcludingInstitutions();

    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.institution IS NULL ORDER BY u.aiPoints DESC")
    List<User> findTopUsersByAiPointsExcludingInstitutions();

    long countByInstitutionIsNullAndIsActiveTrue();

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

    // Clean-up queries for user deletion
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM team_members WHERE team_id IN (SELECT id FROM teams WHERE created_by = :id) OR user_id = :id", nativeQuery = true)
    void deleteTeamMembersByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM teams WHERE created_by = :id", nativeQuery = true)
    void deleteTeamsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM exam_problems WHERE exam_id IN (SELECT id FROM exams WHERE faculty_id = :id)", nativeQuery = true)
    void deleteExamProblemsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM exams WHERE faculty_id = :id", nativeQuery = true)
    void deleteExamsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM anti_cheat_logs WHERE student_id = :id", nativeQuery = true)
    void deleteAntiCheatLogsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ai_interviews WHERE student_id = :id", nativeQuery = true)
    void deleteAiInterviewsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM plagiarism_reports WHERE submission_1_id IN (SELECT id FROM problem_submissions WHERE user_id = :id) OR submission_2_id IN (SELECT id FROM problem_submissions WHERE user_id = :id)", nativeQuery = true)
    void deletePlagiarismReportsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM problem_submissions WHERE user_id = :id", nativeQuery = true)
    void deleteProblemSubmissionsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM quiz_submissions WHERE user_id = :id", nativeQuery = true)
    void deleteQuizSubmissionsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM ai_analysis_reports WHERE execution_record_id IN (SELECT id FROM execution_records WHERE user_id = :id)", nativeQuery = true)
    void deleteAiAnalysisReportsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM execution_records WHERE user_id = :id", nativeQuery = true)
    void deleteExecutionRecordsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM chat_messages WHERE sender_id = :id OR recipient_id = :id", nativeQuery = true)
    void deleteChatMessagesByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM resume_educations WHERE resume_id IN (SELECT id FROM resumes WHERE user_id = :id)", nativeQuery = true)
    void deleteEducationByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM resume_experiences WHERE resume_id IN (SELECT id FROM resumes WHERE user_id = :id)", nativeQuery = true)
    void deleteExperienceByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM resume_projects WHERE resume_id IN (SELECT id FROM resumes WHERE user_id = :id)", nativeQuery = true)
    void deleteProjectsByUserId(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM resumes WHERE user_id = :id", nativeQuery = true)
    void deleteResumeByUserId(@Param("id") Long id);
}
