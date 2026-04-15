package com.example.demo.controller.api;

import com.example.demo.model.Attendance;
import com.example.demo.service.AttendanceService;
import com.example.demo.service.StudentService;
import com.example.demo.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceRestController {

    private final AttendanceService attendanceService;
    private final StudentService studentService;
    private final SubjectService subjectService;

    @GetMapping("/student/{studentId}")
    public List<Attendance> getAttendanceByStudent(@PathVariable Long studentId) {
        return attendanceService.findByStudentId(studentId);
    }

    @PostMapping
    public ResponseEntity<Attendance> addAttendance(@RequestBody Attendance attendance, 
                                                   @RequestParam Long studentId, 
                                                   @RequestParam Long subjectId) {
        attendance.setStudent(studentService.findById(studentId).orElseThrow());
        attendance.setSubject(subjectService.findById(subjectId).orElseThrow());
        if (attendance.getDate() == null) {
            attendance.setDate(LocalDate.now());
        }
        return ResponseEntity.ok(attendanceService.save(attendance));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttendance(@PathVariable Long id) {
        attendanceService.delete(id);
        return ResponseEntity.ok().build();
    }
}
