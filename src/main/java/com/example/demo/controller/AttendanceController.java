package com.example.demo.controller;

import com.example.demo.model.Attendance;
import com.example.demo.model.AttendanceStatus;
import com.example.demo.model.Student;
import com.example.demo.model.Subject;
import com.example.demo.service.AttendanceService;
import com.example.demo.service.StudentService;
import com.example.demo.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final StudentService studentService;
    private final SubjectService subjectService;

    @GetMapping("/student/{studentId}")
    public String viewAttendanceForStudent(@PathVariable Long studentId, Model model) {
        Student student = studentService.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + studentId));

        List<Attendance> attendances = attendanceService.findByStudentId(studentId);
        
        // Dynamiczne wyliczanie statystyk matematycznych i objętości frekwencji w obiekcie
        long total = attendances.size();
        long present = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.OBECNOSC).count();
        long late = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.SPOZNIENIE).count();
        long absent = attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.NIEOBECNOSC).count();
        
        double percentage = 0.0;
        if (total > 0) {
            percentage = (double) (present + late) / total * 100.0;
        }

        Attendance newAttendance = new Attendance();
        newAttendance.setStudent(student);
        newAttendance.setDate(LocalDate.now());

        model.addAttribute("student", student);
        model.addAttribute("attendances", attendances);
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("statuses", AttendanceStatus.values());
        model.addAttribute("newAttendance", newAttendance);
        
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

    @GetMapping("/delete/{id}")
    public String deleteAttendance(@PathVariable Long id) {
        Attendance attendance = attendanceService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance Id:" + id));
        Long studentId = attendance.getStudent().getId();
        attendanceService.delete(id);
        return "redirect:/attendance/student/" + studentId;
    }
}
