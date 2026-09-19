package com.kartik.terminal.repository;

import com.kartik.terminal.entity.Problem;
import com.kartik.terminal.entity.ProblemSubmission;
import com.kartik.terminal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProblemSubmissionRepository extends JpaRepository<ProblemSubmission, Long> {
    List<ProblemSubmission> findByUserOrderBySubmittedAtDesc(User user);
    List<ProblemSubmission> findByProblemOrderBySubmittedAtDesc(Problem problem);
    List<ProblemSubmission> findByUserAndProblemOrderBySubmittedAtDesc(User user, Problem problem);
    Optional<ProblemSubmission> findFirstByUserAndProblemAndIsSolvedTrue(User user, Problem problem);
    boolean existsByUserAndProblemAndIsSolvedTrue(User user, Problem problem);
    long countByProblemAndIsSolvedTrue(Problem problem);
    void deleteByProblemId(Long problemId);
}
