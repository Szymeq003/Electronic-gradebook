package com.example.demo.service;

import com.example.demo.model.Schedule;
import com.example.demo.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;

    public List<Schedule> getScheduleForClass(Long classId) {
        return scheduleRepository.findBySchoolClassId(classId);
    }

    public List<Schedule> getScheduleForTeacher(Long teacherId) {
        return scheduleRepository.findBySubjectTeacherId(teacherId);
    }
}
