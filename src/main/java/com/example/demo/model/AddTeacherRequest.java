package com.example.demo.model;

import lombok.Data;

@Data
public class AddTeacherRequest {

    private String firstName;
    private String lastName;
    private String email;

    // Login credentials
    private String username;
    private String password;
}
