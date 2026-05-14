package com.example.demo.service;

import com.example.demo.model.AppUser;
import com.example.demo.model.Message;
import com.example.demo.model.Role;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public List<Message> getInbox(AppUser user) {
        return messageRepository.findByRecipientOrderBySentAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Message> getSentMessages(AppUser user) {
        return messageRepository.findBySenderOrderBySentAtDesc(user);
    }

    @Transactional
    public Message getMessage(Long id, AppUser reader) {
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Wiadomość nie istnieje"));

        if (!message.getSender().equals(reader) && !message.getRecipient().equals(reader)) {
            throw new IllegalStateException("Nie masz uprawnień do przeczytania tej wiadomości");
        }

        if (message.getRecipient().equals(reader) && !message.isRead()) {
            message.setRead(true);
            messageRepository.save(message);
        }

        return message;
    }

    @Transactional
    public void sendMessage(AppUser sender, Long recipientId, String subject, String content) {
        AppUser recipient = appUserRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Odbiorca nie istnieje"));

        if (!canSendTo(sender, recipient)) {
            throw new IllegalStateException("Nie masz uprawnień do wysłania wiadomości do tego użytkownika");
        }

        Message message = Message.builder()
                .sender(sender)
                .recipient(recipient)
                .subject(subject)
                .content(content)
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        messageRepository.save(message);
    }

    public boolean canSendTo(AppUser sender, AppUser recipient) {
        if (recipient.getRole() == Role.ROLE_ADMIN) {
            return false;
        }

        if (sender.getRole() == Role.ROLE_STUDENT) {
            return recipient.getRole() == Role.ROLE_TEACHER || recipient.getRole() == Role.ROLE_SECRETARY;
        }

        return true;
    }

    @Transactional(readOnly = true)
    public List<AppUser> getAvailableRecipients(AppUser currentUser) {
        return appUserRepository.findAllDistinct().stream()
                .distinct()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .filter(user -> canSendTo(currentUser, user))
                .sorted(Comparator.comparingInt(this::getRolePriority)
                        .thenComparing(user -> getDisplayName(user)))
                .collect(Collectors.toList());
    }

    private int getRolePriority(AppUser user) {
        switch (user.getRole()) {
            case ROLE_DIRECTOR: return 1;
            case ROLE_SECRETARY: return 2;
            case ROLE_TEACHER: return 3;
            case ROLE_STUDENT: return 4;
            default: return 5;
        }
    }

    public String getDisplayName(AppUser user) {
        if (user.getTeacher() != null) {
            return user.getTeacher().getLastName() + " " + user.getTeacher().getFirstName();
        }
        if (user.getStudent() != null) {
            return user.getStudent().getLastName() + " " + user.getStudent().getFirstName();
        }
        return user.getUsername();
    }

    public String getRoleDisplayName(AppUser user) {
        switch (user.getRole()) {
            case ROLE_DIRECTOR:  return "Dyrekcja";
            case ROLE_SECRETARY: return "Sekretariat";
            case ROLE_TEACHER:   return "Nauczyciel";
            case ROLE_STUDENT:   return "Uczeń";
            default:             return user.getRole().name();
        }
    }
}
