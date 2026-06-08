package com.example.demo.controller;

import com.example.demo.model.Schedule;
import com.example.demo.model.SchoolClass;
import com.example.demo.model.Student;
import com.example.demo.model.Teacher;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.SchoolClassRepository;
import com.example.demo.repository.StudentRepository;
import com.example.demo.repository.TeacherRepository;
import com.example.demo.service.HolidayService;
import com.example.demo.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final HolidayService holidayService;
    private final SchoolClassRepository schoolClassRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final AppUserRepository appUserRepository;
    private final ScheduleRepository scheduleRepository;

    @GetMapping
    public String index(Authentication authentication, Model model) {
        if (authentication != null) {
            String username = authentication.getName();
            if (hasRole(authentication, "ROLE_TEACHER")) {
                return appUserRepository.findByUsername(username)
                        .map(u -> u.getTeacher() != null
                                ? "redirect:/schedules/teacher/" + u.getTeacher().getId()
                                : "redirect:/teacher/dashboard")
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

        // Wyznacz backUrl na podstawie roli
        String backUrl = "/admin";
        if (authentication != null) {
            if (hasRole(authentication, "ROLE_DIRECTOR")) backUrl = "/director/dashboard";
            else if (hasRole(authentication, "ROLE_SECRETARY")) backUrl = "/secretary/dashboard";
        }

        model.addAttribute("classes", schoolClassRepository.findAll());
        model.addAttribute("teachers", teacherRepository.findAll());
        model.addAttribute("backUrl", backUrl);
        return "schedule_index";
    }

    @GetMapping("/class/{id}")
    public String classSchedule(@PathVariable Long id,
                                @RequestParam(defaultValue = "0") int week,
                                Authentication authentication, Model model) {
        boolean isPrivileged = hasRole(authentication, "ROLE_ADMIN")
                || hasRole(authentication, "ROLE_DIRECTOR")
                || hasRole(authentication, "ROLE_SECRETARY");

        if (!isPrivileged) {
            if (hasRole(authentication, "ROLE_STUDENT")) {
                Student student = appUserRepository.findByUsername(authentication.getName())
                        .map(u -> u.getStudent()).orElse(null);
                if (student == null || student.getSchoolClass() == null
                        || !student.getSchoolClass().getId().equals(id)) {
                    return student != null && student.getSchoolClass() != null
                            ? "redirect:/schedules/class/" + student.getSchoolClass().getId()
                            : "redirect:/student/dashboard";
                }
            } else if (hasRole(authentication, "ROLE_TEACHER")) {
                // Nauczyciel może zobaczyć plan klasy, jeśli uczy w niej jakiegoś przedmiotu
                Teacher teacher = appUserRepository.findByUsername(authentication.getName())
                        .map(u -> u.getTeacher()).orElse(null);
                if (teacher == null) {
                    return "redirect:/teacher/dashboard";
                }
                // findBySchoolClassId używa JOIN FETCH — brak lazy loading exception
                boolean teachesInClass = scheduleRepository.findBySchoolClassId(id).stream()
                        .anyMatch(s -> s.getSubject() != null
                                && s.getSubject().getTeacher() != null
                                && s.getSubject().getTeacher().getId().equals(teacher.getId()));
                if (!teachesInClass) {
                    return "redirect:/teacher/dashboard";
                }
            } else {
                return "redirect:/schedules";
            }
        }

        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Klasa nie znaleziona: " + id));
        List<Schedule> schedules = scheduleService.getScheduleForClass(id);
        Map<String, List<Schedule>> scheduleByDay = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getDayOfWeek().name()));

        model.addAttribute("scheduleByDay", scheduleByDay);
        model.addAttribute("title", "Plan lekcji – klasa " + schoolClass.getName());
        model.addAttribute("entityType", "class");
        model.addAttribute("backUrl", isPrivileged ? "/schedules"
                : hasRole(authentication, "ROLE_TEACHER") ? "/teacher/classes"
                : "/student/dashboard");
        addHolidayContext(model, week);
        return "schedule";
    }

    @GetMapping("/teacher/{id}")
    public String teacherSchedule(@PathVariable Long id,
                                  @RequestParam(defaultValue = "0") int week,
                                  Authentication authentication, Model model) {
        boolean isPrivileged = hasRole(authentication, "ROLE_ADMIN")
                || hasRole(authentication, "ROLE_DIRECTOR")
                || hasRole(authentication, "ROLE_SECRETARY");

        if (!isPrivileged) {
            if (hasRole(authentication, "ROLE_TEACHER")) {
                Teacher teacher = appUserRepository.findByUsername(authentication.getName())
                        .map(u -> u.getTeacher()).orElse(null);
                if (teacher == null || !teacher.getId().equals(id)) {
                    return teacher != null
                            ? "redirect:/schedules/teacher/" + teacher.getId()
                            : "redirect:/teacher/dashboard";
                }
            } else {
                return "redirect:/schedules";
            }
        }

        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nauczyciel nie znaleziony: " + id));
        List<Schedule> schedules = scheduleService.getScheduleForTeacher(id);
        Map<String, List<Schedule>> scheduleByDay = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getDayOfWeek().name()));

        model.addAttribute("scheduleByDay", scheduleByDay);
        model.addAttribute("title", "Plan lekcji – " + teacher.getFirstName() + " " + teacher.getLastName());
        model.addAttribute("entityType", "teacher");
        model.addAttribute("backUrl", isPrivileged ? "/schedules" : "/teacher/dashboard");
        addHolidayContext(model, week);
        return "schedule";
    }

    /** Dodaje do modelu: listę dat pon–pt bieżącego tygodnia, mapę świąt i offset tygodnia. */
    private void addHolidayContext(Model model, int weekOffset) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today
                .with(WeekFields.ISO.dayOfWeek(), DayOfWeek.MONDAY.getValue())
                .plusWeeks(weekOffset);
        LocalDate friday = monday.plusDays(4);

        List<LocalDate> weekDates = new ArrayList<>();
        for (int i = 0; i < 5; i++) weekDates.add(monday.plusDays(i));

        Map<LocalDate, String> holidays = holidayService.getHolidaysInRange(monday, friday);

        model.addAttribute("today", today);
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("holidays", holidays);
        model.addAttribute("weekOffset", weekOffset);
        model.addAttribute("mondayDate", monday);
        model.addAttribute("fridayDate", friday);
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}
