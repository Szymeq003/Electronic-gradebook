package com.example.demo.controller;

import com.example.demo.repository.TeacherRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.SchoolClassRepository;
import com.example.demo.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/director")
@RequiredArgsConstructor
public class DirectorController {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("teacherCount", teacherRepository.count());
        model.addAttribute("studentCount", studentRepository.count());
        model.addAttribute("classCount", schoolClassRepository.count());
        model.addAttribute("subjectCount", subjectRepository.count());
        model.addAttribute("role", "DYREKCJA");
        return "director_dashboard";
    }
}
