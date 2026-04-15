package com.example.demo.controller;

import com.example.demo.model.SchoolClass;
import com.example.demo.model.Teacher;
import com.example.demo.repository.SchoolClassRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.service.ScheduleService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public String index(Model model) {
        model.addAttribute("classes", schoolClassRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        return "schedule_index";
    }

    @GetMapping("/class/{id}")
    public String classSchedule(@PathVariable Long id, Model model) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class Id:" + id));
        model.addAttribute("schedules", scheduleService.getScheduleForClass(id));
        model.addAttribute("title", "Plan Lekcji Klasy: " + schoolClass.getName());
        model.addAttribute("entityType", "class");
        return "schedule";
    }

    @GetMapping("/teacher/{id}")
    public String teacherSchedule(@PathVariable Long id, Model model) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid teacher Id:" + id));
        model.addAttribute("schedules", scheduleService.getScheduleForTeacher(id));
        model.addAttribute("title", "Plan Lekcji Nauczyciela: " + teacher.getFirstName() + " " + teacher.getLastName());
        model.addAttribute("entityType", "teacher");
        return "schedule";
    }
}
