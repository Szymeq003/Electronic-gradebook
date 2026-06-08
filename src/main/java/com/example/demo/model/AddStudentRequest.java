package com.example.demo.model;

import lombok.Data;

@Data
public class AddStudentRequest {

    private String firstName;
    private String lastName;
    private String email;
    private Long schoolClassId;

    // Login credentials
    private String username;
    private String password;
}
