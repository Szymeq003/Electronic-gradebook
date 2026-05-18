package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import com.example.demo.config.MessageEncryptionConverter;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
    @Index(name = "idx_message_sender", columnList = "sender_id"),
    @Index(name = "idx_message_recipient", columnList = "recipient_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private AppUser sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private AppUser recipient;

    @Column(nullable = false)
    @Convert(converter = MessageEncryptionConverter.class)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Convert(converter = MessageEncryptionConverter.class)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean isRead = false;
}
