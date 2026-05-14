package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.GradeRepository;
import com.example.demo.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentDashboardController {

    private final SecurityService securityService;
    private final ExamRepository examRepository;
    private final GradeRepository gradeRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Student student = securityService.getCurrentStudent()
                .orElseThrow(() -> new IllegalStateException("Logged in user is not a student or student profile is missing"));

        // Calculate GPA in DB
        Double gpaVal = gradeRepository.getAverageGradeByStudentId(student.getId());
        double gpa = (gpaVal != null) ? gpaVal : 0.0;

        // Recent Grades (last 5)
        List<Grade> allGrades = student.getGrades();
        List<Grade> recentGrades = allGrades.stream()
                .sorted(Comparator.comparing(Grade::getDate).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Upcoming Exams
        List<Exam> upcomingExams = examRepository.findBySchoolClassIdAndDateGreaterThanEqualOrderByDateAsc(
                student.getSchoolClass().getId(), LocalDate.now());

        model.addAttribute("student", student);
        model.addAttribute("gpa", String.format("%.2f", gpa));
        model.addAttribute("recentGrades", recentGrades);
        model.addAttribute("upcomingExams", upcomingExams);

        return "student_dashboard";
    }

    @GetMapping("/grades")
    public String viewAllGrades(Model model) {
        Student student = securityService.getCurrentStudent().orElseThrow();
        
        // Group grades by subject
        java.util.Map<Subject, List<Grade>> gradesBySubject = student.getGrades().stream()
                .collect(Collectors.groupingBy(Grade::getSubject));

        model.addAttribute("student", student);
        model.addAttribute("gradesBySubject", gradesBySubject);
        return "student_grades_view";
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        Student student = securityService.getCurrentStudent()
                .orElseThrow(() -> new IllegalStateException("Zalogowany użytkownik nie jest uczniem"));
        
        if (student.getSchoolClass() == null) {
            return "redirect:/student/dashboard";
        }
        
        return "redirect:/schedules/class/" + student.getSchoolClass().getId();
    }

    @GetMapping("/attendance")
    public String attendance(Model model) {
        Student student = securityService.getCurrentStudent()
                .orElseThrow(() -> new IllegalStateException("Zalogowany użytkownik nie jest uczniem"));
        
        return "redirect:/attendance/student/" + student.getId();
    }
}
