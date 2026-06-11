package com.example.demo.controller;

import com.example.demo.model.Subject;
import com.example.demo.service.SubjectService;
import com.example.demo.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;
    private final TeacherService teacherService;

    @GetMapping
    public String listSubjects(Model model, org.springframework.security.core.Authentication authentication) {
        String backUrl = "/admin";
        String backText = "← Powrót do panelu admina";

        if (authentication != null) {
            if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SECRETARY"))) {
                backUrl = "/secretary/dashboard";
                backText = "← Powrót do sekretariatu";
            } else if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR"))) {
                backUrl = "/director/dashboard";
                backText = "← Powrót do panelu dyrektora";
            }
        }

        model.addAttribute("backUrl", backUrl);
        model.addAttribute("backText", backText);
        model.addAttribute("subjects", subjectService.findAll());
        return "subjects";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("newSubject", new Subject());
        model.addAttribute("teachers", teacherService.findAll());
        return "add_subject";
    }

    @PostMapping("/add")
    public String addSubject(@ModelAttribute Subject newSubject) {
        subjectService.save(newSubject);
        return "redirect:/admin/subjects";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Subject subject = subjectService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject Id:" + id));
        model.addAttribute("subject", subject);
        model.addAttribute("teachers", teacherService.findAll());
        return "edit_subject";
    }

    @PostMapping("/edit/{id}")
    public String updateSubject(@PathVariable Long id, @ModelAttribute Subject subject) {
        subject.setId(id);
        subjectService.save(subject);
        return "redirect:/admin/subjects";
    }

    @GetMapping("/delete/{id}")
    public String deleteSubject(@PathVariable Long id) {
        subjectService.delete(id);
        return "redirect:/admin/subjects";
    }
}
