package com.example.demo.controller;

import com.example.demo.model.AppUser;
import com.example.demo.model.Attendance;
import com.example.demo.model.AttendanceStatus;
import com.example.demo.model.Role;
import com.example.demo.model.Student;
import com.example.demo.model.Subject;
import com.example.demo.model.Teacher;
import com.example.demo.model.Schedule;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.service.MessageService;
import com.example.demo.service.AttendanceService;
import com.example.demo.service.SecurityService;
import com.example.demo.service.StudentService;
import com.example.demo.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final StudentService studentService;
    private final SubjectService subjectService;
    private final SecurityService securityService;
    private final ScheduleRepository scheduleRepository;
    private final AppUserRepository appUserRepository;
    private final MessageService messageService;

    @GetMapping("/student/{studentId}")
    public String viewAttendanceForStudent(@PathVariable Long studentId,
                                           @RequestParam(defaultValue = "0") int week,
                                           Model model) {
        // Security check for students
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_STUDENT) {
                if (user.getStudent() == null || !user.getStudent().getId().equals(studentId)) {
                    throw new IllegalStateException("Brak uprawnień do przeglądania obecności innego ucznia");
                }
            }
        });

        Student student = studentService.findByIdWithClass(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + studentId));

        List<Attendance> attendances = attendanceService.findByStudentId(studentId);
        
        // Dynamiczne wyliczanie statystyk matematycznych i objętości frekwencji w obiekcie
        long total = attendances.size();
        long present = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long late = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long absent = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();
        
        double percentage = 0.0;
        if (total > 0) {
            percentage = (double) (present + late) / total * 100.0;
        }

        Attendance newAttendance = new Attendance();
        newAttendance.setStudent(student);
        newAttendance.setDate(LocalDate.now());

        List<Subject> filteredSubjects;
        boolean canAddAttendance = false;
        Long currentTeacherId = null;
        String currentUserRole = "";
        boolean isTutor = false;
        Optional<AppUser> currentUserOpt = securityService.getCurrentAppUser();
        if (currentUserOpt.isPresent()) {
            AppUser currentUser = currentUserOpt.get();
            currentUserRole = currentUser.getRole().name();
            if (currentUser.getRole() == Role.ROLE_ADMIN 
                    || currentUser.getRole() == Role.ROLE_DIRECTOR 
                    || currentUser.getRole() == Role.ROLE_SECRETARY) {
                filteredSubjects = subjectService.findAll();
                canAddAttendance = true;
            } else if (currentUser.getRole() == Role.ROLE_TEACHER && currentUser.getTeacher() != null) {
                Teacher teacher = currentUser.getTeacher();
                currentTeacherId = teacher.getId();
                filteredSubjects = scheduleRepository.findBySchoolClassId(student.getSchoolClass().getId()).stream()
                        .map(Schedule::getSubject)
                        .filter(sub -> sub != null && sub.getTeacher() != null && sub.getTeacher().getId().equals(teacher.getId()))
                        .distinct()
                        .collect(Collectors.toList());
                canAddAttendance = !filteredSubjects.isEmpty();
                
                Teacher classTutor = student.getSchoolClass() != null ? student.getSchoolClass().getTeacher() : null;
                if (classTutor != null && classTutor.getId().equals(teacher.getId())) {
                    isTutor = true;
                }
            } else {
                filteredSubjects = List.of();
            }
        } else {
            filteredSubjects = List.of();
        }

        // Filtrowanie wyświetlanej listy obecności dla nauczyciela (nie-wychowawcy)
        if (currentUserOpt.isPresent()) {
            AppUser currentUser = currentUserOpt.get();
            if (currentUser.getRole() == Role.ROLE_TEACHER && currentUser.getTeacher() != null) {
                Teacher teacher = currentUser.getTeacher();
                if (!isTutor) {
                    attendances = attendances.stream()
                            .filter(a -> a.getSubject() != null 
                                      && a.getSubject().getTeacher() != null 
                                      && a.getSubject().getTeacher().getId().equals(teacher.getId()))
                            .collect(Collectors.toList());
                }
            }
        }

        // Przygotowanie kontekstu tygodniowego
        LocalDate today = LocalDate.now();
        LocalDate monday = today
                .with(WeekFields.ISO.dayOfWeek(), DayOfWeek.MONDAY.getValue())
                .plusWeeks(week);
        LocalDate friday = monday.plusDays(4);

        List<LocalDate> weekDates = new ArrayList<>();
        for (int i = 0; i < 5; i++) weekDates.add(monday.plusDays(i));

        // Plan lekcji
        List<Schedule> schedules = List.of();
        if (student.getSchoolClass() != null) {
            schedules = scheduleRepository.findBySchoolClassId(student.getSchoolClass().getId());
        }
        Map<String, List<Schedule>> scheduleByDay = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getDayOfWeek().name()));

        // Mapa obecności: "data_subjectId" -> Attendance
        Map<String, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(
                        a -> a.getDate().toString() + "_" + a.getSubject().getId(),
                        a -> a,
                        (existing, replacement) -> existing
                ));

        model.addAttribute("student", student);
        model.addAttribute("attendances", attendances);
        model.addAttribute("subjects", filteredSubjects);
        model.addAttribute("statuses", AttendanceStatus.values());
        model.addAttribute("newAttendance", newAttendance);
        model.addAttribute("canAddAttendance", canAddAttendance);
        model.addAttribute("currentUserRole", currentUserRole);
        model.addAttribute("currentTeacherId", currentTeacherId);
        model.addAttribute("isTutor", isTutor);

        model.addAttribute("today", today);
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("weekOffset", week);
        model.addAttribute("mondayDate", monday);
        model.addAttribute("fridayDate", friday);
        model.addAttribute("scheduleByDay", scheduleByDay);
        model.addAttribute("attendanceMap", attendanceMap);
        
        model.addAttribute("statTotal", total);
        model.addAttribute("statPresent", present);
        model.addAttribute("statLate", late);
        model.addAttribute("statAbsent", absent);
        model.addAttribute("statPercentage", percentage);

        return "attendance_student";
    }

    @PostMapping("/add")
    public String addAttendance(@ModelAttribute Attendance newAttendance, 
                                @RequestParam("studentId") Long studentId, 
                                @RequestParam("subjectId") Long subjectId) {
        // Blokada dla uczniów
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_STUDENT) {
                throw new IllegalStateException("Uczniowie nie mogą modyfikować obecności");
            }
        });

        Student student = studentService.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + studentId));
        Subject subject = subjectService.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject Id:" + subjectId));
        
        newAttendance.setStudent(student);
        newAttendance.setSubject(subject);
        
        if (newAttendance.getDate() == null) {
            newAttendance.setDate(LocalDate.now());
        }
        
        attendanceService.save(newAttendance);
        return "redirect:/attendance/student/" + studentId;
    }

    @PostMapping("/delete/{id}")
    public String deleteAttendance(@PathVariable Long id) {
        // Blokada dla uczniów
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_STUDENT) {
                throw new IllegalStateException("Uczniowie nie mogą modyfikować obecności");
            }
        });

        Attendance attendance = attendanceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance Id:" + id));
        Long studentId = attendance.getStudent().getId();
        attendanceService.delete(id);
        return "redirect:/attendance/student/" + studentId;
    }

    @PostMapping("/excuse/request/{id}")
    public String requestExcuse(@PathVariable Long id) {
        Attendance attendance = attendanceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance Id:" + id));

        AppUser currentUser = securityService.getCurrentAppUser()
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie jest zalogowany"));

        // Tylko sam uczeń może poprosić o usprawiedliwienie swojej nieobecności
        if (currentUser.getRole() == Role.ROLE_STUDENT) {
            if (currentUser.getStudent() == null || !currentUser.getStudent().getId().equals(attendance.getStudent().getId())) {
                throw new IllegalStateException("Brak uprawnień do usprawiedliwienia nieobecności innego ucznia");
            }
        }

        if (attendance.getStatus() != AttendanceStatus.ABSENT) {
            throw new IllegalStateException("Można usprawiedliwiać tylko nieobecności");
        }

        attendance.setExcuseRequested(true);
        attendanceService.saveExcuse(attendance);

        // Wyślij automatyczną wiadomość systemową do wychowawcy klasy, jeśli istnieje
        if (attendance.getStudent().getSchoolClass() != null && attendance.getStudent().getSchoolClass().getTeacher() != null) {
            Teacher tutor = attendance.getStudent().getSchoolClass().getTeacher();
            Optional<AppUser> tutorUserOpt = appUserRepository.findByTeacherId(tutor.getId());
            if (tutorUserOpt.isPresent()) {
                AppUser tutorUser = tutorUserOpt.get();
                String subjectText = "Prośba o usprawiedliwienie nieobecności - " + attendance.getStudent().getFirstName() + " " + attendance.getStudent().getLastName();
                String contentText = String.format(
                        "Dzień dobry,\n\nProszę o usprawiedliwienie nieobecności w dniu %s na lekcji przedmiotu %s.\n\nZ poważaniem,\n%s %s",
                        attendance.getDate().toString(),
                        attendance.getSubject().getName(),
                        attendance.getStudent().getFirstName(),
                        attendance.getStudent().getLastName()
                );
                messageService.sendMessage(currentUser, tutorUser.getId(), subjectText, contentText);
            }
        }

        return "redirect:/attendance/student/" + attendance.getStudent().getId();
    }

    @PostMapping("/excuse/approve/{id}")
    public String approveExcuse(@PathVariable Long id) {
        Attendance attendance = attendanceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance Id:" + id));

        AppUser currentUser = securityService.getCurrentAppUser()
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie jest zalogowany"));

        // Sprawdzenie uprawnień: tylko wychowawca klasy lub admin/sekretariat/dyrekcja
        boolean isPrivileged = currentUser.getRole() == Role.ROLE_ADMIN 
                || currentUser.getRole() == Role.ROLE_DIRECTOR 
                || currentUser.getRole() == Role.ROLE_SECRETARY;
        
        if (!isPrivileged) {
            if (currentUser.getRole() == Role.ROLE_TEACHER && currentUser.getTeacher() != null) {
                Teacher teacher = currentUser.getTeacher();
                Teacher tutor = attendance.getStudent().getSchoolClass() != null ? attendance.getStudent().getSchoolClass().getTeacher() : null;
                if (tutor == null || !tutor.getId().equals(teacher.getId())) {
                    throw new IllegalStateException("Tylko wychowawca klasy może zaakceptować to usprawiedliwienie");
                }
            } else {
                throw new IllegalStateException("Brak uprawnień do zaakceptowania usprawiedliwienia");
            }
        }

        attendance.setStatus(AttendanceStatus.EXCUSED);
        attendance.setExcuseRequested(false);
        attendanceService.saveExcuse(attendance);

        return "redirect:/attendance/student/" + attendance.getStudent().getId();
    }

    @PostMapping("/excuse/reject/{id}")
    public String rejectExcuse(@PathVariable Long id) {
        Attendance attendance = attendanceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance Id:" + id));

        AppUser currentUser = securityService.getCurrentAppUser()
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie jest zalogowany"));

        // Sprawdzenie uprawnień: tylko wychowawca klasy lub admin/sekretariat/dyrekcja
        boolean isPrivileged = currentUser.getRole() == Role.ROLE_ADMIN 
                || currentUser.getRole() == Role.ROLE_DIRECTOR 
                || currentUser.getRole() == Role.ROLE_SECRETARY;
        
        if (!isPrivileged) {
            if (currentUser.getRole() == Role.ROLE_TEACHER && currentUser.getTeacher() != null) {
                Teacher teacher = currentUser.getTeacher();
                Teacher tutor = attendance.getStudent().getSchoolClass() != null ? attendance.getStudent().getSchoolClass().getTeacher() : null;
                if (tutor == null || !tutor.getId().equals(teacher.getId())) {
                    throw new IllegalStateException("Tylko wychowawca klasy może odrzucić to usprawiedliwienie");
                }
            } else {
                throw new IllegalStateException("Brak uprawnień do odrzucenia usprawiedliwienia");
            }
        }

        attendance.setExcuseRequested(false);
        attendanceService.saveExcuse(attendance);

        return "redirect:/attendance/student/" + attendance.getStudent().getId();
    }
}
