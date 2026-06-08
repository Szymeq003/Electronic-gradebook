package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("teachers", teacherService.findAll());
        return "index";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("addTeacherRequest", new AddTeacherRequest());
        return "add_teacher";
    }

    @PostMapping("/add")
    public String addTeacher(@ModelAttribute AddTeacherRequest req, RedirectAttributes redirectAttributes) {
        // Validate username uniqueness
        if (appUserRepository.findByUsername(req.getUsername()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Nazwa użytkownika '" + req.getUsername() + "' jest już zajęta.");
            return "redirect:/admin/teachers/add";
        }

        // Create Teacher entity
        Teacher teacher = new Teacher();
        teacher.setFirstName(req.getFirstName());
        teacher.setLastName(req.getLastName());
        teacher.setEmail(req.getEmail());
        Teacher savedTeacher = teacherService.save(teacher);

        // Create AppUser
        AppUser user = new AppUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.ROLE_TEACHER);
        user.setTeacher(savedTeacher);
        appUserRepository.save(user);

        return "redirect:/admin/teachers";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Teacher teacher = teacherService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid teacher Id:" + id));
        model.addAttribute("teacher", teacher);
        return "edit_teacher";
    }

    @PostMapping("/edit/{id}")
    public String updateTeacher(@PathVariable Long id, @ModelAttribute Teacher teacher) {
        teacher.setId(id);
        teacherService.save(teacher);
        return "redirect:/admin/teachers";
    }

    @GetMapping("/delete/{id}")
    public String deleteTeacher(@PathVariable Long id) {
        teacherService.delete(id);
        return "redirect:/admin/teachers";
    }
}
