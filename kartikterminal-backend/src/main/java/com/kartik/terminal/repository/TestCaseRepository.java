package com.kartik.terminal.repository;

import com.kartik.terminal.entity.Problem;
import com.kartik.terminal.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByProblemOrderByIdAsc(Problem problem);
    List<TestCase> findByProblemIdOrderByIdAsc(Long problemId);
    void deleteByProblemId(Long problemId);
}
