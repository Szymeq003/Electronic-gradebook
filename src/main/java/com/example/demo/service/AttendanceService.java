package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.model.Attendance;
import com.example.demo.model.Role;
import com.example.demo.model.Student;
import com.example.demo.model.Subject;
import com.example.demo.model.Teacher;
import com.example.demo.repository.AttendanceRepository;
import com.example.demo.repository.ScheduleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ScheduleRepository scheduleRepository;
    private final SecurityService securityService;

    public void checkAttendanceWriteAccess(Attendance attendance) {
        if (attendance == null) return;
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_TEACHER) {
                Teacher teacher = user.getTeacher();
                if (teacher == null) {
                    throw new IllegalStateException("Brak powiązanego profilu nauczyciela");
                }
                
                Subject subject = attendance.getSubject();
                if (subject == null || subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
                    throw new IllegalStateException("Nauczyciel może wstawiać obecność tylko ze swojego przedmiotu");
                }
                
                Student student = attendance.getStudent();
                if (student == null || student.getSchoolClass() == null) {
                    throw new IllegalStateException("Niepoprawny uczeń lub klasa");
                }
                
                LocalDate date = attendance.getDate();
                if (date == null) {
                    throw new IllegalStateException("Niepoprawna data obecności");
                }
                
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                boolean hasLesson = scheduleRepository.findBySchoolClassId(student.getSchoolClass().getId()).stream()
                        .anyMatch(s -> s.getSubject() != null 
                                    && s.getSubject().getId().equals(subject.getId()) 
                                    && s.getDayOfWeek() == dayOfWeek);
                
                if (!hasLesson) {
                    throw new IllegalStateException("Nauczyciel może wstawiać obecność tylko z godziny gdzie ma zajęcia z klasą");
                }
            }
        });
    }

    public void checkAttendanceDeleteAccess(Attendance attendance) {
        if (attendance == null) return;
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_TEACHER) {
                Teacher teacher = user.getTeacher();
                if (teacher == null) {
                    throw new IllegalStateException("Brak powiązanego profilu nauczyciela");
                }
                
                // Sprawdź czy to wychowawca klasy ucznia
                Teacher tutor = (attendance.getStudent() != null && attendance.getStudent().getSchoolClass() != null)
                        ? attendance.getStudent().getSchoolClass().getTeacher() : null;
                boolean isTutor = (tutor != null && tutor.getId().equals(teacher.getId()));
                
                if (!isTutor) {
                    Subject subject = attendance.getSubject();
                    if (subject == null || subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId())) {
                        throw new IllegalStateException("Nauczyciel może usuwać obecność tylko ze swojego przedmiotu lub jako wychowawca klasy");
                    }
                }
            }
        });
    }

    public List<Attendance> findAll() {
        return attendanceRepository.findAll();
    }

    public Optional<Attendance> findById(Long id) {
        return attendanceRepository.findById(id);
    }

    public Attendance save(Attendance attendance) {
        checkAttendanceWriteAccess(attendance);
        return attendanceRepository.save(attendance);
    }

    @Transactional
    public Attendance saveExcuse(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    public void delete(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid attendance Id:" + id));
        checkAttendanceDeleteAccess(attendance);
        attendanceRepository.delete(attendance);
    }

    public List<Attendance> findByStudentId(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }
    
    public List<Attendance> findByStudentIdAndSubjectId(Long studentId, Long subjectId) {
        return attendanceRepository.findByStudentIdAndSubjectId(studentId, subjectId);
    }
}
