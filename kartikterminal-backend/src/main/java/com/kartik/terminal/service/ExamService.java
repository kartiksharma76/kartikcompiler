package com.kartik.terminal.service;

import com.kartik.terminal.entity.Exam;
import com.kartik.terminal.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {
    private final ExamRepository examRepository;

    public Exam createExam(Exam exam) {
        return examRepository.save(exam);
    }

    public List<Exam> getExamsByFaculty(Long facultyId) {
        return examRepository.findByFacultyId(facultyId);
    }
    
    public Exam getExamById(Long id) {
        return examRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Exam not found"));
    }
}
