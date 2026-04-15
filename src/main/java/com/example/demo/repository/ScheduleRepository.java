package com.example.demo.repository;

import com.example.demo.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByRoomId(Long roomId);
    List<Schedule> findBySchoolClassId(Long schoolClassId);
    List<Schedule> findBySubjectTeacherId(Long teacherId);
}
