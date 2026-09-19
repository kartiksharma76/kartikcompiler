package com.kartik.terminal.repository;

import com.kartik.terminal.entity.Institution;
import com.kartik.terminal.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findAllByOrderByIdDesc();
    List<Problem> findByInstitutionIsNullOrderByIdDesc();
    List<Problem> findByInstitutionOrderByIdDesc(Institution institution);
    List<Problem> findByInstitutionOrInstitutionIsNullOrderByIdDesc(Institution institution);
    boolean existsByTitleIgnoreCase(String title);
}
