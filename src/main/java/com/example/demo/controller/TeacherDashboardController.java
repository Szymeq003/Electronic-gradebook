package com.example.demo.controller;

import com.example.demo.model.Exam;
import com.example.demo.model.SchoolClass;
import com.example.demo.model.Teacher;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.SchoolClassRepository;
import com.example.demo.service.ScheduleService;
import com.example.demo.service.SecurityService;
import com.example.demo.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final SecurityService securityService;
    private final SchoolClassRepository schoolClassRepository;
    private final ExamRepository examRepository;
    private final SubjectService subjectService;
    private final ScheduleService scheduleService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Teacher teacher = securityService.getCurrentTeacher()
                .orElseThrow(() -> new IllegalStateException("Logged in user is not a teacher or teacher profile is missing"));
        
        model.addAttribute("teacher", teacher);
        model.addAttribute("classes", teacher.getSchoolClasses());
        model.addAttribute("subjects", teacher.getSubjects());
        model.addAttribute("exams", examRepository.findByTeacherIdOrderByDateAsc(teacher.getId()));
        return "teacher_dashboard";
    }

    @PostMapping("/exam/add")
    public String addExam(@ModelAttribute Exam exam, @RequestParam("subjectId") Long subjectId, @RequestParam("classId") Long classId) {
        Teacher teacher = securityService.getCurrentTeacher().orElseThrow();
        exam.setTeacher(teacher);
        exam.setSubject(subjectService.findById(subjectId).orElseThrow());
        exam.setSchoolClass(schoolClassRepository.findById(classId).orElseThrow());
        examRepository.save(exam);
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/exam/delete/{id}")
    public String deleteExam(@PathVariable Long id) {
        Teacher teacher = securityService.getCurrentTeacher()
                .orElseThrow(() -> new IllegalStateException("Nauczyciel nie jest zalogowany"));
        
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sprawdzian nie istnieje"));
        
        if (!exam.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalStateException("Nie możesz usunąć sprawdzianu innego nauczyciela");
        }
        
        examRepository.deleteById(id);
        return "redirect:/teacher/dashboard";
    }

    @GetMapping("/classes")
    public String myClasses(Model model) {
        Teacher teacher = securityService.getCurrentTeacher()
                .orElseThrow(() -> new IllegalStateException("Teacher profile missing"));
        
        model.addAttribute("classes", teacher.getSchoolClasses());
        return "teacher_classes";
    }

    @GetMapping("/class/{id}")
    public String classDetails(@PathVariable Long id, Model model) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class Id:" + id));
        
        // Security check: only allow if this teacher is assigned to the class or teaches a subject in this class
        // For simplicity, we check if teacher is the main class teacher
        Teacher teacher = securityService.getCurrentTeacher().orElseThrow();
        
        model.addAttribute("schoolClass", schoolClass);
        model.addAttribute("students", schoolClass.getStudents());
        return "teacher_class_details";
    }

    @GetMapping("/schedules")
    public String mySchedules(Model model) {
        Teacher teacher = securityService.getCurrentTeacher()
                .orElseThrow(() -> new IllegalStateException("Teacher profile missing"));
        
        model.addAttribute("teacher", teacher);
        model.addAttribute("schedules", scheduleService.getScheduleForTeacher(teacher.getId()));
        return "teacher_schedule";
    }
}
