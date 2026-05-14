package com.example.demo.controller;

import com.example.demo.model.SchoolClass;

import com.example.demo.model.Student;
import com.example.demo.model.Teacher;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.SchoolClassRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping
    public String index(Authentication authentication, Model model) {
        if (authentication != null) {
            String username = authentication.getName();
            
            if (hasRole(authentication, "ROLE_TEACHER")) {
                return appUserRepository.findByUsername(username)
                        .map(u -> u.getTeacher() != null ? "redirect:/schedules/teacher/" + u.getTeacher().getId() : "redirect:/teacher/dashboard")
                        .orElse("redirect:/teacher/dashboard");
            }
            
            if (hasRole(authentication, "ROLE_STUDENT")) {
                return appUserRepository.findByUsername(username)
                        .map(u -> (u.getStudent() != null && u.getStudent().getSchoolClass() != null) 
                            ? "redirect:/schedules/class/" + u.getStudent().getSchoolClass().getId() 
                            : "redirect:/student/dashboard")
                        .orElse("redirect:/student/dashboard");
            }
        }

        model.addAttribute("classes", schoolClassRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        return "schedule_index";
    }

    @GetMapping("/class/{id}")
    public String classSchedule(@PathVariable Long id, Authentication authentication, Model model) {
        // Security check
        if (!hasRole(authentication, "ROLE_ADMIN")) {
            if (hasRole(authentication, "ROLE_STUDENT")) {
                Student student = appUserRepository.findByUsername(authentication.getName())
                        .map(u -> u.getStudent()).orElse(null);
                if (student == null || student.getSchoolClass() == null || !student.getSchoolClass().getId().equals(id)) {
                    return student != null && student.getSchoolClass() != null 
                           ? "redirect:/schedules/class/" + student.getSchoolClass().getId() 
                           : "redirect:/student/dashboard";
                }
            } else {
                // Teachers cannot see class schedules based on strict requirement
                return "redirect:/schedules"; 
            }
        }

        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class Id:" + id));
        model.addAttribute("schedules", scheduleService.getScheduleForClass(id));
        model.addAttribute("title", "Plan Lekcji Klasy: " + schoolClass.getName());
        model.addAttribute("entityType", "class");
        return "schedule";
    }

    @GetMapping("/teacher/{id}")
    public String teacherSchedule(@PathVariable Long id, Authentication authentication, Model model) {
        // Security check
        if (!hasRole(authentication, "ROLE_ADMIN")) {
            if (hasRole(authentication, "ROLE_TEACHER")) {
                Teacher teacher = appUserRepository.findByUsername(authentication.getName())
                        .map(u -> u.getTeacher()).orElse(null);
                if (teacher == null || !teacher.getId().equals(id)) {
                    return teacher != null ? "redirect:/schedules/teacher/" + teacher.getId() : "redirect:/teacher/dashboard";
                }
            } else {
                // Students cannot see teacher schedules
                return "redirect:/schedules";
            }
        }

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid teacher Id:" + id));
        model.addAttribute("schedules", scheduleService.getScheduleForTeacher(id));
        model.addAttribute("title", "Plan Lekcji Nauczyciela: " + teacher.getFirstName() + " " + teacher.getLastName());
        model.addAttribute("entityType", "teacher");
        return "schedule";
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
