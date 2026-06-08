package com.example.demo.service;

import com.example.demo.model.Grade;
import com.example.demo.repository.GradeRepository;
import com.example.demo.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private GradeService gradeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gradeService, "editWindowHours", 3);
    }

    @Test
    void testIsEditableWithNullCreatedAt() {
        Grade grade = new Grade();
        grade.setCreatedAt(null);

        assertFalse(gradeService.isEditable(grade));
        assertEquals(0, gradeService.minutesUntilLocked(grade));
    }

    @Test
    void testIsEditableWithinWindow() {
        Grade grade = new Grade();
        grade.setCreatedAt(LocalDateTime.now().minusHours(1));

        assertTrue(gradeService.isEditable(grade));
        assertTrue(gradeService.minutesUntilLocked(grade) > 0);
        assertTrue(gradeService.minutesUntilLocked(grade) <= 120);
    }

    @Test
    void testIsEditableOutsideWindow() {
        Grade grade = new Grade();
        grade.setCreatedAt(LocalDateTime.now().minusHours(4));

        assertFalse(gradeService.isEditable(grade));
        assertEquals(0, gradeService.minutesUntilLocked(grade));
    }
}
