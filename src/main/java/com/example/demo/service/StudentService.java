package com.example.demo.service;

import com.example.demo.model.Student;
import com.example.demo.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    public Optional<Student> findByIdWithClass(Long id) {
        return studentRepository.findByIdWithClass(id);
    }

    public Student save(Student student) {
        return studentRepository.save(student);
    }

    public void delete(Long id) {
        studentRepository.deleteById(id);
    }

    public List<Student> findBySchoolClassName(String schoolClass) {
        return studentRepository.findBySchoolClassName(schoolClass);
    }

    public List<Student> findByClassIdAndSearch(Long classId, String search) {
        return studentRepository.findByClassIdAndSearch(classId, search);
    }
}
