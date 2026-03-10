package com.example.demo.controller;

import com.example.demo.repository.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/classes")
@RequiredArgsConstructor
public class SchoolClassController {

    private final SchoolClassRepository schoolClassRepository;

    @GetMapping
    public String listClasses(Model model) {
        model.addAttribute("classes", schoolClassRepository.findAll());
        return "classes";
    }
}
