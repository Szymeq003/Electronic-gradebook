package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Sprawdzanie struktury bazy danych (SQLite)...");
        try {
            // Dodanie kolumny excuse_requested do tabeli attendances, jeśli nie istnieje
            jdbcTemplate.execute("ALTER TABLE attendances ADD COLUMN excuse_requested INTEGER DEFAULT 0");
            log.info("Pomyślnie dodano kolumnę excuse_requested do tabeli attendances.");
        } catch (Exception e) {
            // Ignorujemy błąd, jeśli kolumna już istnieje
            log.debug("Kolumna excuse_requested prawdopodobnie już istnieje: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("UPDATE attendances SET excuse_requested = 0 WHERE excuse_requested IS NULL");
            log.info("Pomyślnie zaktualizowano wartości NULL w kolumnie excuse_requested na 0.");
        } catch (Exception e) {
            log.warn("Nie udało się zaktualizować wartości NULL w kolumnie excuse_requested: {}", e.getMessage());
        }
    }
}
