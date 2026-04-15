package com.example.demo.controller.api;

import com.example.demo.model.Grade;
import com.example.demo.service.GradeService;
import com.example.demo.service.StudentService;
import com.example.demo.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class GradeRestController {

    private final GradeService gradeService;
    private final StudentService studentService;
    private final SubjectService subjectService;

    @GetMapping("/student/{studentId}")
    public List<Grade> getGradesByStudent(@PathVariable Long studentId) {
        return gradeService.findByStudentId(studentId);
    }

    @GetMapping("/student/{studentId}/subject/{subjectId}")
    public List<Grade> getGradesByStudentAndSubject(@PathVariable Long studentId, @PathVariable Long subjectId) {
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
