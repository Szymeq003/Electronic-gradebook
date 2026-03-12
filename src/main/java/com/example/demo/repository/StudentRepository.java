package com.example.demo.repository;

import com.example.demo.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findBySchoolClassName(String className);

    @Query("SELECT s FROM Student s WHERE (:classId IS NULL OR s.schoolClass.id = :classId) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Student> findByClassIdAndSearch(@Param("classId") Long classId, @Param("search") String search);
}
