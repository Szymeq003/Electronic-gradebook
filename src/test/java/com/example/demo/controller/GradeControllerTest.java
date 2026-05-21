package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class GradeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GradeService gradeService;

    @Mock
    private StudentService studentService;

    @Mock
    private SubjectService subjectService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private GradeController gradeController;

    private Student student;
    private Subject subject;
    private Grade grade;
    private AppUser teacherUser;
    private AppUser adminUser;

    @BeforeEach
    void setUp() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(gradeController)
                .setViewResolvers(viewResolver)
                .build();

        student = new Student();
        student.setId(1L);
        student.setFirstName("Jan");
        student.setLastName("Kowalski");

        subject = new Subject();
        subject.setId(1L);
        subject.setName("Matematyka");

        grade = new Grade();
        grade.setId(1L);
        grade.setValue("5");
        grade.setStudent(student);
        grade.setSubject(subject);
        grade.setDate(LocalDate.now());
        grade.setCreatedAt(LocalDateTime.now().minusHours(1));

        teacherUser = new AppUser();
        teacherUser.setUsername("nauczyciel1");
        teacherUser.setRole(Role.ROLE_TEACHER);

        adminUser = new AppUser();
        adminUser.setUsername("admin");
        adminUser.setRole(Role.ROLE_ADMIN);
    }

    @Test
    void testViewGradesForSubjectPopulatesModel() throws Exception {
        when(studentService.findById(1L)).thenReturn(Optional.of(student));
        when(subjectService.findById(1L)).thenReturn(Optional.of(subject));
        when(gradeService.findByStudentIdAndSubjectId(1L, 1L)).thenReturn(Collections.singletonList(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(teacherUser));
        when(gradeService.isEditable(grade)).thenReturn(true);
        when(gradeService.minutesUntilLocked(grade)).thenReturn(120L);

        mockMvc.perform(get("/grades/student/1/subject/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("student_grades"))
                .andExpect(model().attributeExists("student"))
                .andExpect(model().attributeExists("subject"))
                .andExpect(model().attributeExists("grades"))
                .andExpect(model().attributeExists("editableMap"))
                .andExpect(model().attributeExists("minutesMap"));
    }

    @Test
    void testShowEditFormWhenEditable() throws Exception {
        when(gradeService.findById(1L)).thenReturn(Optional.of(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(teacherUser));
        when(gradeService.isEditable(grade)).thenReturn(true);

        mockMvc.perform(get("/grades/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit_grade"))
                .andExpect(model().attributeExists("grade"));
    }

    @Test
    void testShowEditFormWhenLockedForTeacher() throws Exception {
        when(gradeService.findById(1L)).thenReturn(Optional.of(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(teacherUser));
        when(gradeService.isEditable(grade)).thenReturn(false);

        mockMvc.perform(get("/grades/edit/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grades/student/1/subject/1"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void testShowEditFormWhenLockedForAdmin() throws Exception {
        when(gradeService.findById(1L)).thenReturn(Optional.of(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(adminUser));

        mockMvc.perform(get("/grades/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit_grade"))
                .andExpect(model().attributeExists("grade"));
    }

    @Test
    void testUpdateGradeWhenEditable() throws Exception {
        when(gradeService.findById(1L)).thenReturn(Optional.of(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(teacherUser));
        when(gradeService.isEditable(grade)).thenReturn(true);

        mockMvc.perform(post("/grades/edit/1")
                .param("value", "5+")
                .param("date", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grades/student/1/subject/1"));

        verify(gradeService, times(1)).save(any(Grade.class));
    }

    @Test
    void testUpdateGradeWhenLockedForTeacher() throws Exception {
        when(gradeService.findById(1L)).thenReturn(Optional.of(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(teacherUser));
        when(gradeService.isEditable(grade)).thenReturn(false);

        mockMvc.perform(post("/grades/edit/1")
                .param("value", "5+")
                .param("date", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grades/student/1/subject/1"))
                .andExpect(flash().attributeExists("error"));

        verify(gradeService, never()).save(any(Grade.class));
    }

    @Test
    void testUpdateGradeWhenLockedForAdmin() throws Exception {
        when(gradeService.findById(1L)).thenReturn(Optional.of(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(adminUser));
        when(gradeService.isEditable(grade)).thenReturn(false);

        mockMvc.perform(post("/grades/edit/1")
                .param("value", "5+")
                .param("date", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grades/student/1/subject/1"));

        verify(gradeService, times(1)).save(any(Grade.class));
    }

    @Test
    void testDeleteGradeWhenEditable() throws Exception {
        when(gradeService.findById(1L)).thenReturn(Optional.of(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(teacherUser));
        when(gradeService.isEditable(grade)).thenReturn(true);

        mockMvc.perform(post("/grades/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grades/student/1/subject/1"));

        verify(gradeService, times(1)).delete(1L);
    }

    @Test
    void testDeleteGradeWhenLockedForTeacher() throws Exception {
        when(gradeService.findById(1L)).thenReturn(Optional.of(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(teacherUser));
        when(gradeService.isEditable(grade)).thenReturn(false);

        mockMvc.perform(post("/grades/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grades/student/1/subject/1"))
                .andExpect(flash().attributeExists("error"));

        verify(gradeService, never()).delete(any(Long.class));
    }

    @Test
    void testDeleteGradeWhenLockedForAdmin() throws Exception {
        when(gradeService.findById(1L)).thenReturn(Optional.of(grade));
        when(securityService.getCurrentAppUser()).thenReturn(Optional.of(adminUser));

        mockMvc.perform(post("/grades/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grades/student/1/subject/1"));

        verify(gradeService, times(1)).delete(1L);
    }

}
