package com.example.demo.repository;

import com.example.demo.model.AppUser;
import com.example.demo.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRecipientOrderBySentAtDesc(AppUser recipient);
    List<Message> findBySenderOrderBySentAtDesc(AppUser sender);
}
