package com.example.demo.repository;

import com.example.demo.model.PolishHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PolishHolidayRepository extends JpaRepository<PolishHoliday, Long> {
    Optional<PolishHoliday> findByDate(LocalDate date);
    boolean existsByDate(LocalDate date);
}
