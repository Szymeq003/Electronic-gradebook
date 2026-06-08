package com.example.demo.controller;

import com.example.demo.model.Exam;
import com.example.demo.model.SchoolClass;
import com.example.demo.model.Teacher;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.SchoolClassRepository;
import com.example.demo.service.SecurityService;
import com.example.demo.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherDashboardController {

    private final SecurityService securityService;
    private final SchoolClassRepository schoolClassRepository;
    private final ExamRepository examRepository;
    private final SubjectService subjectService;
    private final ScheduleRepository scheduleRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Teacher teacher = securityService.getCurrentTeacher()
                .orElseThrow(() -> new IllegalStateException("Logged in user is not a teacher or teacher profile is missing"));

        // Wszystkie wpisy planu lekcji dla tego nauczyciela (z JOIN FETCH — bez lazy loading)
        List<com.example.demo.model.Schedule> teacherSchedules =
                scheduleRepository.findBySubjectTeacherId(teacher.getId());

        // Klasy gdzie nauczyciel uczy (distinct, posortowane)
        List<SchoolClass> taughtClasses = teacherSchedules.stream()
                .map(s -> s.getSchoolClass())
                .filter(c -> c != null)
                .distinct()
                .sorted(java.util.Comparator.comparing(SchoolClass::getName))
                .collect(Collectors.toList());

        // Mapa classId -> lista przedmiotów (do JS-owego filtrowania w formularzu sprawdzianu)
        // Format JSON: { "1": [{"id":5,"name":"Matematyka"}], "2": [...], ... }
        StringBuilder classSubjectsJson = new StringBuilder("{");
        java.util.Map<Long, java.util.LinkedHashSet<String>> seen = new java.util.HashMap<>();
        java.util.Map<Long, java.util.List<String>> classSubjectEntries = new java.util.LinkedHashMap<>();

        for (com.example.demo.model.Schedule s : teacherSchedules) {
            if (s.getSchoolClass() == null || s.getSubject() == null) continue;
            Long cid = s.getSchoolClass().getId();
            Long sid = s.getSubject().getId();
            String sname = s.getSubject().getName().replace("\"", "\\\"");
            String key = cid + "_" + sid;
            seen.computeIfAbsent(cid, k -> new java.util.LinkedHashSet<>());
            if (seen.get(cid).add(key)) {
                classSubjectEntries.computeIfAbsent(cid, k -> new java.util.ArrayList<>())
                        .add("{\"id\":" + sid + ",\"name\":\"" + sname + "\"}");
            }
        }
        boolean firstClass = true;
        for (java.util.Map.Entry<Long, java.util.List<String>> e : classSubjectEntries.entrySet()) {
            if (!firstClass) classSubjectsJson.append(",");
            classSubjectsJson.append("\"").append(e.getKey()).append("\":[")
                    .append(String.join(",", e.getValue())).append("]");
            firstClass = false;
        }
        classSubjectsJson.append("}");

        model.addAttribute("teacher", teacher);
        model.addAttribute("classes", taughtClasses);
        model.addAttribute("subjects", teacher.getSubjects());
        model.addAttribute("classSubjectsJson", classSubjectsJson.toString());
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

        // Wszystkie klasy gdzie nauczyciel uczy jakiegoś przedmiotu (przez plan lekcji)
        List<SchoolClass> taughtClasses = scheduleRepository.findBySubjectTeacherId(teacher.getId()).stream()
                .map(s -> s.getSchoolClass())
                .filter(c -> c != null)
                .distinct()
                .sorted(java.util.Comparator.comparing(SchoolClass::getName))
                .collect(Collectors.toList());

        model.addAttribute("classes", taughtClasses);
        model.addAttribute("teacher", teacher);
        return "teacher_classes";
    }

    @GetMapping("/class/{id}")
    public String classDetails(@PathVariable Long id, Model model) {
        Teacher teacher = securityService.getCurrentTeacher().orElseThrow();
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid class Id:" + id));

        // Kontrola dostępu: nauczyciel musi uczyć w tej klasie
        boolean teachesInClass = scheduleRepository.findBySchoolClassId(id).stream()
                .anyMatch(s -> s.getSubject() != null
                        && s.getSubject().getTeacher() != null
                        && s.getSubject().getTeacher().getId().equals(teacher.getId()));
        if (!teachesInClass) {
            return "redirect:/teacher/classes";
        }

        model.addAttribute("schoolClass", schoolClass);
        model.addAttribute("students", schoolClass.getStudents());
        return "teacher_class_details";
    }

    @GetMapping("/schedules")
    public String mySchedules() {
        Teacher teacher = securityService.getCurrentTeacher()
                .orElseThrow(() -> new IllegalStateException("Teacher profile missing"));
        return "redirect:/schedules/teacher/" + teacher.getId();
    }
}
