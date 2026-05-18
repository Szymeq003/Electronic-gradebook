package com.example.demo.config;

import com.example.demo.model.Message;
import com.example.demo.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JEDNORAZOWY RUNNER — migracja istniejących wiadomości do formatu zaszyfrowanego.
 * Po pierwszym uruchomieniu ustaw app.migration.enabled=false w application.properties.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MessageMigrationRunner {

    private final MessageRepository messageRepository;

    @org.springframework.beans.factory.annotation.Value("${app.migration.enabled:false}")
    private boolean migrationEnabled;

    @Bean
    public CommandLineRunner migratePlaintextMessages() {
        return args -> {
            if (!migrationEnabled) {
                log.info("Migracja wiadomości pominięta (app.migration.enabled=false)");
                return;
            }
            log.info("Rozpoczynanie migracji istniejących wiadomości do formatu zaszyfrowanego...");
            try {
                var messages = messageRepository.findAll();
                int migratedCount = 0;
                for (Message msg : messages) {
                    // Wymuszenie ponownego zapisu encji — JPA wywoła konwerter i automatycznie zaszyfruje dane.
                    // Wiadomości już zaszyfrowane zostaną bezpiecznie ponownie zaszyfrowane z nowym losowym IV.
                    messageRepository.save(msg);
                    migratedCount++;
                }
                log.info("Zakończono sukcesem migrację {} wiadomości.", migratedCount);
                log.warn("Migracja zakończona! Ustaw app.migration.enabled=false w pliku application.properties i zrestartuj aplikację.");
            } catch (Exception e) {
                log.error("Błąd podczas migracji wiadomości: ", e);
            }
        };
    }
}
