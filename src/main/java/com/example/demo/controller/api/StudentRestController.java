package com.example.demo.controller.api;

import com.example.demo.model.Student;
import com.example.demo.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentRestController {

    private final StudentService studentService;

    @GetMapping
    public List<Student> getAllStudents(@RequestParam(required = false) Long classId, 
                                      @RequestParam(required = false) String search) {
        return studentService.findByClassIdAndSearch(classId, search);
    }

    @GetMapping("/{id}")
    public Student getStudentById(@PathVariable Long id) {
        return studentService.findById(id).orElseThrow();
    }
}
