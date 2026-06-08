package com.example.demo.repository;

import com.example.demo.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    @Query("SELECT DISTINCT u FROM AppUser u " +
           "LEFT JOIN FETCH u.student s " +
           "LEFT JOIN FETCH u.teacher t " +
           "LEFT JOIN FETCH s.grades")
    List<AppUser> findAllDistinct();
    Optional<AppUser> findByTeacherId(Long teacherId);
    Optional<AppUser> findByStudentId(Long studentId);
}
