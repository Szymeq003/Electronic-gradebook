package com.example.demo.repository;

import com.example.demo.model.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    @EntityGraph(attributePaths = {"schoolClass"})
    List<Student> findBySchoolClassName(String className);

    @EntityGraph(attributePaths = {"schoolClass"})
    @Query("SELECT s FROM Student s WHERE (:classId IS NULL OR s.schoolClass.id = :classId) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Student> findByClassIdAndSearch(@Param("classId") Long classId, @Param("search") String search);
    @EntityGraph(attributePaths = {"schoolClass"})
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findByIdWithClass(@Param("id") Long id);
}
