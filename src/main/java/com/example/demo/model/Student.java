package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name")
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    private String firstName;

    @Column(name = "last_name")
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    private String lastName;

    @ManyToOne
    @JoinColumn(name = "school_class_id")
    private SchoolClass schoolClass;

    @Column(name = "email")
    private String email;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Grade> grades = new ArrayList<>();
}
