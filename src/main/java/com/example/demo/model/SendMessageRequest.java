package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class SendMessageRequest {

    @NotEmpty(message = "Należy wybrać co najmniej jednego adresata")
    private List<Long> recipientIds;

    @NotBlank(message = "Temat nie może być pusty")
    @Size(max = 200, message = "Temat może mieć maksymalnie 200 znaków")
    private String subject;

    @NotBlank(message = "Treść wiadomości nie może być pusta")
    @Size(max = 5000, message = "Treść może mieć maksymalnie 5000 znaków")
    private String content;
}

