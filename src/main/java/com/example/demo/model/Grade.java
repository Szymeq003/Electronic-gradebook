package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "grades", indexes = {
    @Index(name = "idx_grade_student", columnList = "student_id"),
    @Index(name = "idx_grade_subject", columnList = "subject_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "value")
    @NotBlank(message = "Grade value is required")
    private String value;

    public Double getNumericValue() {
        if (value == null) return 0.0;
        switch (value) {
            case "1": return 1.0;
            case "1+": return 1.5;
            case "2-": return 1.75;
            case "2": return 2.0;
            case "2+": return 2.5;
            case "3-": return 2.75;
            case "3": return 3.0;
            case "3+": return 3.5;
            case "4-": return 3.75;
            case "4": return 4.0;
            case "4+": return 4.5;
            case "5-": return 4.75;
            case "5": return 5.0;
            case "5+": return 5.5;
            case "6-": return 5.75;
            case "6": return 6.0;
            default: return 0.0;
        }
    }

    @Column(name = "date")
    private LocalDate date = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = java.time.LocalDateTime.now();
        }
    }
}

