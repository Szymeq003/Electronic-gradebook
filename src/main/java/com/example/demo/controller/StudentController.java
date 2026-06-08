package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.SchoolClassRepository;
import com.example.demo.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final SchoolClassRepository schoolClassRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

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
        model.addAttribute("addStudentRequest", new AddStudentRequest());
        model.addAttribute("schoolClasses", schoolClassRepository.findAll());
        return "add_student";
    }

    @PostMapping("/add")
    public String addStudent(@ModelAttribute AddStudentRequest req, RedirectAttributes redirectAttributes) {
        // Validate username uniqueness
        if (appUserRepository.findByUsername(req.getUsername()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Nazwa użytkownika '" + req.getUsername() + "' jest już zajęta.");
            return "redirect:/admin/students/add";
        }

        // Create Student entity
        Student student = new Student();
        student.setFirstName(req.getFirstName());
        student.setLastName(req.getLastName());
        student.setEmail(req.getEmail());
        if (req.getSchoolClassId() != null) {
            schoolClassRepository.findById(req.getSchoolClassId())
                    .ifPresent(student::setSchoolClass);
        }
        Student savedStudent = studentService.save(student);

        // Create AppUser
        AppUser user = new AppUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.ROLE_STUDENT);
        user.setStudent(savedStudent);
        appUserRepository.save(user);

        return "redirect:/admin/students";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Student student = studentService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + id));
        model.addAttribute("student", student);
        model.addAttribute("schoolClasses", schoolClassRepository.findAll());
        // Pass existing AppUser if any
        appUserRepository.findByStudentId(id)
                .ifPresent(u -> model.addAttribute("appUser", u));
        return "edit_student";
    }

    @PostMapping("/edit/{id}")
    public String updateStudent(@PathVariable Long id, @ModelAttribute Student student) {
        student.setId(id);
        studentService.save(student);
        return "redirect:/admin/students";
    }

    @GetMapping("/delete/{id}")
    public String deleteStudent(@PathVariable Long id) {
        studentService.delete(id);
        return "redirect:/admin/students";
    }
}
