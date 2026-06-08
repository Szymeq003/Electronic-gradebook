package com.example.demo.controller.api;

import com.example.demo.model.AppUser;
import com.example.demo.model.Grade;
import com.example.demo.model.Role;
import com.example.demo.model.Teacher;
import com.example.demo.service.GradeService;
import com.example.demo.service.SecurityService;
import com.example.demo.service.StudentService;
import com.example.demo.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeRestController {

    private final GradeService gradeService;
    private final StudentService studentService;
    private final SubjectService subjectService;
    private final SecurityService securityService;

    @GetMapping("/student/{studentId}")
    public List<Grade> getGradesByStudent(@PathVariable Long studentId) {
        List<Grade> grades = gradeService.findByStudentId(studentId);
        Optional<AppUser> currentUserOpt = securityService.getCurrentAppUser();
        if (currentUserOpt.isPresent() && currentUserOpt.get().getRole() == Role.ROLE_TEACHER) {
            Teacher teacher = currentUserOpt.get().getTeacher();
            if (teacher != null) {
                return grades.stream()
                        .filter(g -> g.getSubject() != null && g.getSubject().getTeacher() != null && g.getSubject().getTeacher().getId().equals(teacher.getId()))
                        .collect(Collectors.toList());
            } else {
                return List.of();
            }
        }
        return grades;
    }

    @GetMapping("/student/{studentId}/subject/{subjectId}")
    public List<Grade> getGradesByStudentAndSubject(@PathVariable Long studentId, @PathVariable Long subjectId) {
        Optional<AppUser> currentUserOpt = securityService.getCurrentAppUser();
        if (currentUserOpt.isPresent() && currentUserOpt.get().getRole() == Role.ROLE_TEACHER) {
            Teacher teacher = currentUserOpt.get().getTeacher();
            if (teacher != null) {
                com.example.demo.model.Subject subject = subjectService.findById(subjectId).orElse(null);
                if (subject == null || subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
                    throw new IllegalStateException("Brak uprawnień do przeglądania ocen z przedmiotu innego nauczyciela");
                }
            } else {
                return List.of();
            }
        }
        return gradeService.findByStudentIdAndSubjectId(studentId, subjectId);
    }

    @PostMapping
    public ResponseEntity<Grade> addGrade(@RequestBody Grade grade, 
                                        @RequestParam Long studentId, 
                                        @RequestParam Long subjectId) {
        grade.setStudent(studentService.findById(studentId).orElseThrow());
        grade.setSubject(subjectService.findById(subjectId).orElseThrow());
        if (grade.getDate() == null) {
            grade.setDate(LocalDate.now());
        }
        return ResponseEntity.ok(gradeService.save(grade));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long id) {
        gradeService.delete(id);
        return ResponseEntity.ok().build();
    }
}
