package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "school_classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true)
    @NotBlank(message = "Class name is required")
    private String name;

    @OneToMany(mappedBy = "schoolClass", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Student> students = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Teacher teacher;
}
