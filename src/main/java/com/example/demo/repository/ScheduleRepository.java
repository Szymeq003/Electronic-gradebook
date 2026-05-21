package com.example.demo.repository;

import com.example.demo.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s " +
           "LEFT JOIN FETCH s.subject sub " +
           "LEFT JOIN FETCH sub.teacher " +
           "LEFT JOIN FETCH s.room " +
           "LEFT JOIN FETCH s.schoolClass " +
           "WHERE s.schoolClass.id = :classId " +
           "ORDER BY s.dayOfWeek, s.startTime")
    List<Schedule> findBySchoolClassId(@Param("classId") Long classId);

    @Query("SELECT s FROM Schedule s " +
           "LEFT JOIN FETCH s.subject sub " +
           "LEFT JOIN FETCH sub.teacher " +
           "LEFT JOIN FETCH s.room " +
           "LEFT JOIN FETCH s.schoolClass " +
           "WHERE sub.teacher.id = :teacherId " +
           "ORDER BY s.dayOfWeek, s.startTime")
    List<Schedule> findBySubjectTeacherId(@Param("teacherId") Long teacherId);

    List<Schedule> findByRoomId(Long roomId);
}
