package com.example.demo.controller;

import com.example.demo.model.AppUser;
import com.example.demo.model.Role;
import com.example.demo.model.SendMessageRequest;
import com.example.demo.service.MessageService;
import com.example.demo.service.SecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SecurityService securityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public String inbox(Model model) {
        AppUser currentUser = securityService.getCurrentAppUser()
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie jest zalogowany"));
        
        model.addAttribute("messages", messageService.getInbox(currentUser));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("view", "inbox");
        model.addAttribute("dashboardUrl", getDashboardUrl(currentUser));
        return "messages";
    }

    @GetMapping("/sent")
    public String sent(Model model) {
        AppUser currentUser = securityService.getCurrentAppUser()
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie jest zalogowany"));
        
        model.addAttribute("messages", messageService.getSentMessages(currentUser));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("view", "sent");
        model.addAttribute("dashboardUrl", getDashboardUrl(currentUser));
        return "messages";
    }

    @GetMapping("/new")
    public String newMessageForm(Model model) {
        AppUser currentUser = securityService.getCurrentAppUser()
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie jest zalogowany"));
        
        prepareNewMessageModel(model, currentUser);
        return "new_message";
    }

    @PostMapping("/send")
    public String sendMessage(@Valid @ModelAttribute SendMessageRequest request,
                              BindingResult bindingResult,
                              Model model) {
        AppUser currentUser = securityService.getCurrentAppUser()
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie jest zalogowany"));

        if (bindingResult.hasErrors()) {
            prepareNewMessageModel(model, currentUser);
            model.addAttribute("errors", bindingResult.getAllErrors());
            return "new_message";
        }

        messageService.sendMessages(currentUser, request.getRecipientIds(), request.getSubject(), request.getContent());
        return "redirect:/messages?sent=true";
    }

    @GetMapping("/{id}")
    public String getMessage(@PathVariable Long id, Model model) {
        AppUser currentUser = securityService.getCurrentAppUser()
                .orElseThrow(() -> new IllegalStateException("Użytkownik nie jest zalogowany"));
        
        model.addAttribute("message", messageService.getMessage(id, currentUser));
        model.addAttribute("currentUser", currentUser);
        return "messages :: messageContent";
    }

    private String getDashboardUrl(AppUser user) {
        switch (user.getRole()) {
            case ROLE_ADMIN:     return "/admin";
            case ROLE_DIRECTOR:  return "/director/dashboard";
            case ROLE_SECRETARY: return "/secretary/dashboard";
            case ROLE_TEACHER:   return "/teacher/dashboard";
            case ROLE_STUDENT:   return "/student/dashboard";
            default:             return "/";
        }
    }

    private void prepareNewMessageModel(Model model, AppUser currentUser) {
        var recipients = messageService.getAvailableRecipients(currentUser);
        Map<Long, String> roleNames = recipients.stream()
                .collect(Collectors.toMap(
                    AppUser::getId,
                    messageService::getRoleDisplayName,
                    (a, b) -> a
                ));
        
        model.addAttribute("recipients", recipients);
        model.addAttribute("roleNames", roleNames);
        model.addAttribute("messageService", messageService);
        model.addAttribute("dashboardUrl", getDashboardUrl(currentUser));

        List<Map<String, String>> recipientsList = recipients.stream()
            .map(r -> {
                Map<String, String> m = new java.util.LinkedHashMap<>();
                m.put("id",    String.valueOf(r.getId()));
                m.put("name",  messageService.getDisplayName(r));
                m.put("role",  messageService.getRoleDisplayName(r));
                m.put("email", r.getStudent() != null ? r.getStudent().getEmail()
                              : r.getTeacher() != null ? r.getTeacher().getEmail()
                              : r.getUsername());
                m.put("klasa", r.getStudent() != null && r.getStudent().getSchoolClass() != null
                              ? r.getStudent().getSchoolClass().getName() : "");
                return m;
            })
            .collect(Collectors.toList());

        try {
            model.addAttribute("recipientsJson", objectMapper.writeValueAsString(recipientsList));
        } catch (Exception e) {
            model.addAttribute("recipientsJson", "[]");
        }
    }
}
