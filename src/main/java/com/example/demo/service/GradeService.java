package com.example.demo.service;

import com.example.demo.model.Grade;
import com.example.demo.repository.GradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;

    @Value("${app.grade.edit-window-hours:3}")
    private int editWindowHours;

    public boolean isEditable(Grade grade) {
        if (grade.getCreatedAt() == null) {
            return false;
        }
        return grade.getCreatedAt().isAfter(LocalDateTime.now().minusHours(editWindowHours));
    }

    public long minutesUntilLocked(Grade grade) {
        if (grade.getCreatedAt() == null) return 0;
        LocalDateTime lockTime = grade.getCreatedAt().plusHours(editWindowHours);
        long minutes = java.time.Duration.between(LocalDateTime.now(), lockTime).toMinutes();
        return Math.max(0, minutes);
    }

    public List<Grade> findAll() {
        return gradeRepository.findAll();
    }

    public Optional<Grade> findById(Long id) {
        return gradeRepository.findById(id);
    }

    public Grade save(Grade grade) {
        return gradeRepository.save(grade);
    }

    public void delete(Long id) {
        gradeRepository.deleteById(id);
    }

    public List<Grade> findByStudentId(Long studentId) {
        return gradeRepository.findByStudentId(studentId);
    }

    public List<Grade> findBySubjectId(Long subjectId) {
        return gradeRepository.findBySubjectId(subjectId);
    }
    public List<Grade> findByStudentIdAndSubjectId(Long studentId, Long subjectId) {
        return gradeRepository.findByStudentIdAndSubjectId(studentId, subjectId);
    }

}
