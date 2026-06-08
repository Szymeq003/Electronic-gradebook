package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.model.Grade;
import com.example.demo.model.Role;
import com.example.demo.model.Teacher;
import com.example.demo.repository.GradeRepository;
import com.example.demo.repository.ScheduleRepository;
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
    private final ScheduleRepository scheduleRepository;
    private final SecurityService securityService;

    @Value("${app.grade.edit-window-hours:3}")
    private int editWindowHours;

    public boolean isEditable(Grade grade) {
        if (grade.getCreatedAt() == null) {
            return false;
        }
        return grade.getCreatedAt().isAfter(LocalDateTime.now().minusHours(editWindowHours))
                || grade.isCorrectionAllowed();
    }

    public long minutesUntilLocked(Grade grade) {
        if (grade.getCreatedAt() == null) return 0;
        LocalDateTime lockTime = grade.getCreatedAt().plusHours(editWindowHours);
        long minutes = java.time.Duration.between(LocalDateTime.now(), lockTime).toMinutes();
        return Math.max(0, minutes);
    }

    public boolean canTeacherWriteGrade(Teacher teacher, Grade grade) {
        if (teacher == null || grade == null || grade.getSubject() == null || grade.getStudent() == null) {
            return false;
        }
        if (grade.getSubject().getTeacher() == null || !grade.getSubject().getTeacher().getId().equals(teacher.getId())) {
            return false;
        }
        if (grade.getStudent().getSchoolClass() == null) {
            return false;
        }
        Long classId = grade.getStudent().getSchoolClass().getId();
        Long subjectId = grade.getSubject().getId();
        return scheduleRepository.findBySchoolClassId(classId).stream()
                .anyMatch(s -> s.getSubject() != null && s.getSubject().getId().equals(subjectId));
    }

    public void checkGradeWriteAccess(Grade grade) {
        if (grade == null) return;
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_TEACHER) {
                Teacher teacher = user.getTeacher();
                if (teacher == null) {
                    throw new IllegalStateException("Brak powiązanego profilu nauczyciela");
                }
                if (!canTeacherWriteGrade(teacher, grade)) {
                    if (grade.getSubject() == null || grade.getSubject().getTeacher() == null || !grade.getSubject().getTeacher().getId().equals(teacher.getId())) {
                        throw new IllegalStateException("Nauczyciel może wystawiać i modyfikować oceny tylko ze swojego przedmiotu");
                    } else {
                        throw new IllegalStateException("Nauczyciel może wystawiać i modyfikować oceny tylko swoim uczniom z klasy, którą uczy");
                    }
                }
            }
        });
    }

    public List<Grade> findAll() {
        return gradeRepository.findAll();
    }

    public Optional<Grade> findById(Long id) {
        return gradeRepository.findById(id);
    }

    public Grade save(Grade grade) {
        checkGradeWriteAccess(grade);
        return gradeRepository.save(grade);
    }

    public void delete(Long id) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid grade Id:" + id));
        checkGradeWriteAccess(grade);
        gradeRepository.delete(grade);
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
