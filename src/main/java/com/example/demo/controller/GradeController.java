package com.example.demo.controller;

import com.example.demo.model.AppUser;
import com.example.demo.model.Grade;
import com.example.demo.model.Role;
import com.example.demo.model.Student;
import com.example.demo.model.Subject;
import com.example.demo.model.Teacher;
import com.example.demo.service.GradeService;
import com.example.demo.service.SecurityService;
import com.example.demo.service.StudentService;
import com.example.demo.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller
@RequestMapping("/grades")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final StudentService studentService;
    private final SubjectService subjectService;
    private final SecurityService securityService;

    private static final Logger log = LoggerFactory.getLogger(GradeController.class);

    private boolean canOverrideLock() {
        return securityService.getCurrentAppUser()
                .map(user -> user.getRole() == Role.ROLE_ADMIN
                          || user.getRole() == Role.ROLE_DIRECTOR
                          || user.getRole() == Role.ROLE_SECRETARY)
                .orElse(false);
    }



    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SubjectSummary {
        private Subject subject;
        private String teacherInitials;
        private List<Grade> grades;
        private double average;
    }

    @GetMapping("/student/{studentId}")
    public String viewSubjectsForStudent(@PathVariable Long studentId, Model model) {
        // Security check for students
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_STUDENT) {
                if (user.getStudent() == null || !user.getStudent().getId().equals(studentId)) {
                    throw new IllegalStateException("Brak uprawnień do przeglądania ocen innego ucznia");
                }
            }
        });

        Student student = studentService.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + studentId));

        List<Subject> allSubjects;
        List<Grade> studentGrades;
        Optional<AppUser> currentUserOpt = securityService.getCurrentAppUser();
        if (currentUserOpt.isPresent() && currentUserOpt.get().getRole() == Role.ROLE_TEACHER) {
            Teacher teacher = currentUserOpt.get().getTeacher();
            if (teacher != null) {
                boolean isTutor = student.getSchoolClass() != null && 
                                  student.getSchoolClass().getTeacher() != null && 
                                  student.getSchoolClass().getTeacher().getId().equals(teacher.getId());

                if (isTutor) {
                    allSubjects = subjectService.findAll();
                    studentGrades = gradeService.findByStudentId(studentId);
                } else {
                    allSubjects = subjectService.findAll().stream()
                            .filter(s -> s.getTeacher() != null && s.getTeacher().getId().equals(teacher.getId()))
                            .collect(Collectors.toList());
                    studentGrades = gradeService.findByStudentId(studentId).stream()
                            .filter(g -> g.getSubject() != null && g.getSubject().getTeacher() != null && g.getSubject().getTeacher().getId().equals(teacher.getId()))
                            .collect(Collectors.toList());
                }
            } else {
                allSubjects = List.of();
                studentGrades = List.of();
            }
        } else {
            allSubjects = subjectService.findAll();
            studentGrades = gradeService.findByStudentId(studentId);
        }

        java.util.Set<Long> takenSubjectIds = studentGrades.stream()
                .map(g -> g.getSubject().getId())
                .collect(java.util.stream.Collectors.toSet());

        java.util.Set<String> takenSubjectNames = studentGrades.stream()
                .map(g -> g.getSubject().getName())
                .collect(java.util.stream.Collectors.toSet());

        java.util.List<SubjectSummary> summaries = new java.util.ArrayList<>();

        boolean isAdminOrSimilar = currentUserOpt.isPresent() && 
            (currentUserOpt.get().getRole() == Role.ROLE_ADMIN || 
             currentUserOpt.get().getRole() == Role.ROLE_SECRETARY || 
             currentUserOpt.get().getRole() == Role.ROLE_DIRECTOR);

        Teacher currentTeacher = (currentUserOpt.isPresent() && currentUserOpt.get().getRole() == Role.ROLE_TEACHER) ? currentUserOpt.get().getTeacher() : null;

        for (Subject sub : allSubjects) {
            if (!takenSubjectIds.contains(sub.getId()) && takenSubjectNames.contains(sub.getName())) {
                continue;
            }

            java.util.List<Grade> gradesForSubject = studentGrades.stream()
                    .filter(g -> g.getSubject().getId().equals(sub.getId()))
                    .collect(java.util.stream.Collectors.toList());

            boolean isSubjectTeacher = currentTeacher != null && sub.getTeacher() != null && sub.getTeacher().getId().equals(currentTeacher.getId());
            if (gradesForSubject.isEmpty() && !isSubjectTeacher && !isAdminOrSimilar) {
                continue;
            }

            double avg = 0.0;
            if (!gradesForSubject.isEmpty()) {
                double sum = 0.0;
                for (Grade g : gradesForSubject) {
                    sum += g.getNumericValue();
                }
                avg = sum / gradesForSubject.size();
            }

            String initials = (sub.getTeacher() != null)
                    ? sub.getTeacher().getFirstName().charAt(0) + "." + sub.getTeacher().getLastName().charAt(0) + "."
                    : "–";
            summaries.add(new SubjectSummary(sub, initials, gradesForSubject, avg));
        }

        model.addAttribute("student", student);
        model.addAttribute("summaries", summaries);
        return "student_subjects";
    }

    @GetMapping("/student/{studentId}/subject/{subjectId}")
    public String viewGradesForSubject(@PathVariable Long studentId, @PathVariable Long subjectId, Model model) {
        // Security check for students
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_STUDENT) {
                if (user.getStudent() == null || !user.getStudent().getId().equals(studentId)) {
                    throw new IllegalStateException("Brak uprawnień do przeglądania ocen innego ucznia");
                }
            }
        });

        Student student = studentService.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + studentId));
        Subject subject = subjectService.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject Id:" + subjectId));

        // Security check for teachers
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_TEACHER) {
                Teacher teacher = user.getTeacher();
                boolean isTutor = student.getSchoolClass() != null && 
                                  student.getSchoolClass().getTeacher() != null && 
                                  student.getSchoolClass().getTeacher().getId().equals(teacher.getId());

                if (!isTutor && (teacher == null || subject.getTeacher() == null || !subject.getTeacher().getId().equals(teacher.getId()))) {
                    throw new IllegalStateException("Brak uprawnień do przeglądania ocen z przedmiotu innego nauczyciela");
                }
            }
        });

        List<Grade> grades = gradeService.findByStudentIdAndSubjectId(studentId, subjectId);

        double average = 0.0;
        if (!grades.isEmpty()) {
            double sum = 0.0;
            for (Grade g : grades) {
                sum += g.getNumericValue();
            }
            average = sum / grades.size();
        }

        Grade newGrade = new Grade();
        newGrade.setStudent(student);
        newGrade.setSubject(subject);
        newGrade.setDate(LocalDate.now());

        Map<Long, Boolean> editableMap = grades.stream()
                .collect(Collectors.toMap(
                        Grade::getId,
                        g -> {
                            boolean baseEditable = canOverrideLock() || gradeService.isEditable(g);
                            Optional<AppUser> currentUser = securityService.getCurrentAppUser();
                            if (currentUser.isPresent() && currentUser.get().getRole() == Role.ROLE_TEACHER) {
                                return baseEditable && gradeService.canTeacherWriteGrade(currentUser.get().getTeacher(), g);
                            }
                            return baseEditable;
                        }
                ));
        Map<Long, Long> minutesMap = grades.stream()
                .collect(Collectors.toMap(
                        Grade::getId,
                        g -> gradeService.minutesUntilLocked(g)
                ));

        boolean canAddGrade = false;
        Optional<AppUser> currentUserOpt = securityService.getCurrentAppUser();
        if (currentUserOpt.isPresent()) {
            AppUser currentUser = currentUserOpt.get();
            if (currentUser.getRole() == Role.ROLE_ADMIN 
                    || currentUser.getRole() == Role.ROLE_DIRECTOR 
                    || currentUser.getRole() == Role.ROLE_SECRETARY) {
                canAddGrade = true;
            } else if (currentUser.getRole() == Role.ROLE_TEACHER && currentUser.getTeacher() != null) {
                Grade tempGrade = new Grade();
                tempGrade.setStudent(student);
                tempGrade.setSubject(subject);
                canAddGrade = gradeService.canTeacherWriteGrade(currentUser.getTeacher(), tempGrade);
            }
        }

        model.addAttribute("student", student);
        model.addAttribute("subject", subject);
        model.addAttribute("grades", grades);
        model.addAttribute("average", average);
        model.addAttribute("newGrade", newGrade);
        model.addAttribute("editableMap", editableMap);
        model.addAttribute("minutesMap", minutesMap);
        model.addAttribute("canAddGrade", canAddGrade);

        return "student_grades";
    }

    @PostMapping("/add")
    public String addGrade(@ModelAttribute Grade newGrade, @RequestParam("studentId") Long studentId,
            @RequestParam("subjectId") Long subjectId) {
        blockStudentWriteAccess();
        Student student = studentService.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student Id:" + studentId));
        Subject subject = subjectService.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid subject Id:" + subjectId));

        newGrade.setStudent(student);
        newGrade.setSubject(subject);

        if (newGrade.getDate() == null) {
            newGrade.setDate(LocalDate.now());
        }

        gradeService.save(newGrade);
        return "redirect:/grades/student/" + studentId + "/subject/" + subjectId;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        blockStudentWriteAccess();
        Grade grade = gradeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid grade Id:" + id));

        Optional<AppUser> currentUserOpt = securityService.getCurrentAppUser();
        if (currentUserOpt.isPresent() && currentUserOpt.get().getRole() == Role.ROLE_TEACHER) {
            Teacher teacher = currentUserOpt.get().getTeacher();
            if (teacher == null || !gradeService.canTeacherWriteGrade(teacher, grade)) {
                throw new IllegalStateException("Brak uprawnień do edycji tej oceny");
            }
        }

        if (!canOverrideLock() && !gradeService.isEditable(grade)) {
            redirectAttributes.addFlashAttribute("error",
                    "Nie można edytować oceny – minęły 3 godziny od jej wystawienia.");
            return "redirect:/grades/student/" + grade.getStudent().getId()
                    + "/subject/" + grade.getSubject().getId();
        }

        model.addAttribute("grade", grade);
        return "edit_grade";
    }

    @PostMapping("/edit/{id}")
    public String updateGrade(@PathVariable Long id, @ModelAttribute Grade updatedGrade, RedirectAttributes redirectAttributes) {
        blockStudentWriteAccess();
        Grade existingGrade = gradeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid grade Id:" + id));

        if (!canOverrideLock() && !gradeService.isEditable(existingGrade)) {
            redirectAttributes.addFlashAttribute("error",
                    "Nie można edytować oceny – minęły 3 godziny od jej wystawienia.");
            return "redirect:/grades/student/" + existingGrade.getStudent().getId()
                    + "/subject/" + existingGrade.getSubject().getId();
        }

        if (canOverrideLock() && !gradeService.isEditable(existingGrade)) {
            String editor = securityService.getCurrentAppUser()
                    .map(u -> u.getUsername()).orElse("nieznany");
            log.warn("AUDIT: użytkownik '{}' edytuje zamrożoną ocenę id={} (wystawiona: {})",
                    editor, existingGrade.getId(), existingGrade.getCreatedAt());
        }

        existingGrade.setValue(updatedGrade.getValue());
        existingGrade.setDate(updatedGrade.getDate());
        existingGrade.setCorrectionAllowed(false);

        gradeService.save(existingGrade);
        return "redirect:/grades/student/" + existingGrade.getStudent().getId() + "/subject/"
                + existingGrade.getSubject().getId();
    }

    @PostMapping("/allow-correction/{id}")
    public String allowCorrection(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean isAuthorized = securityService.getCurrentAppUser()
                .map(user -> user.getRole() == Role.ROLE_ADMIN
                          || user.getRole() == Role.ROLE_DIRECTOR
                          || user.getRole() == Role.ROLE_SECRETARY)
                .orElse(false);
        if (!isAuthorized) {
            throw new IllegalStateException("Brak uprawnień do zmiany statusu oceny");
        }

        Grade grade = gradeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Niepoprawne ID oceny: " + id));

        grade.setCorrectionAllowed(true);
        gradeService.save(grade);

        redirectAttributes.addFlashAttribute("success", "Zezwolono nauczycielowi na poprawę oceny.");
        return "redirect:/grades/student/" + grade.getStudent().getId() + "/subject/" + grade.getSubject().getId();
    }

    @PostMapping("/delete/{id}")
    public String deleteGrade(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        blockStudentWriteAccess();
        Grade grade = gradeService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid grade Id:" + id));

        if (!canOverrideLock() && !gradeService.isEditable(grade)) {
            redirectAttributes.addFlashAttribute("error",
                    "Nie można usunąć oceny – minęły 3 godziny od jej wystawienia.");
            return "redirect:/grades/student/" + grade.getStudent().getId()
                    + "/subject/" + grade.getSubject().getId();
        }

        Long studentId = grade.getStudent().getId();
        Long subjectId = grade.getSubject().getId();

        gradeService.delete(id);
        return "redirect:/grades/student/" + studentId + "/subject/" + subjectId;
    }

    private void blockStudentWriteAccess() {
        securityService.getCurrentAppUser().ifPresent(user -> {
            if (user.getRole() == Role.ROLE_STUDENT) {
                throw new IllegalStateException("Uczniowie nie mogą modyfikować ocen");
            }
        });
    }
}
