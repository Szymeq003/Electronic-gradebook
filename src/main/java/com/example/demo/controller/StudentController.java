package com.example.demo.controller;

import com.example.demo.model.Student;
import com.example.demo.repository.SchoolClassRepository;
import com.example.demo.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final SchoolClassRepository schoolClassRepository;

    @GetMapping
    public String listStudents(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String search,
            Model model) {
        model.addAttribute("students", studentService.findByClassIdAndSearch(classId, search));
        model.addAttribute("schoolClasses", schoolClassRepository.findAll());
        model.addAttribute("selectedClassId", classId);
        model.addAttribute("searchQuery", search);
        return "students";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("newStudent", new Student());
        model.addAttribute("schoolClasses", schoolClassRepository.findAll());
        return "add_student";
    }

    @PostMapping("/add")
    public String addStudent(@ModelAttribute Student newStudent) {
        studentService.save(newStudent);
        return "redirect:/students";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Student student = studentService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + id));
        model.addAttribute("student", student);
        model.addAttribute("schoolClasses", schoolClassRepository.findAll());
        return "edit_student";
    }

    @PostMapping("/edit/{id}")
    public String updateStudent(@PathVariable Long id, @ModelAttribute Student student) {
        student.setId(id);
        studentService.save(student);
        return "redirect:/students";
    }

    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id) {
        studentService.delete(id);
        return "redirect:/students";
    }
}
