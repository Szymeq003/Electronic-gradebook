package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final RoomRepository roomRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public void run(String... args) {
        if (teacherRepository.count() > 0) {
            return;
        }

        Random r = new Random(42);

        String[] firstNamesM = { "Jan", "Piotr", "Tomasz", "Krzysztof", "Michal", "Maciej", "Dawid", "Kamil", "Filip",
                "Szymon", "Marek", "Pawel", "Robert", "Grzegorz", "Lukasz", "Marcin" };
        String[] firstNamesF = { "Anna", "Maria", "Katarzyna", "Malgorzata", "Agnieszka", "Barbara", "Ewa", "Krystyna",
                "Joanna", "Monika", "Izabela", "Magdalena", "Beata", "Dorota", "Halina" };
        String[] lastNames = { "Nowak", "Kowalski", "Wisniewski", "Wojcik", "Kowalczyk", "Kaminski", "Lewandowski",
                "Zielinski", "Szymanski", "Wozniak", "Dabrowski", "Kozlowski", "Jankowski", "Mazur", "Krawczyk",
                "Kaczmarek", "Piotrowski", "Grabowski", "Zajac", "Pawlowski", "Michalski", "Nowicki", "Adamczyk",
                "Dudek", "Zwiec", "Wieczorek", "Mroz", "Stepien", "Olszewski", "Jaworski", "Maliszewski", "Gajewski" };

        List<Teacher> teachers = new ArrayList<>();
        int tCount = 0;
        while (tCount < 30) {
            boolean isMale = r.nextBoolean();
            String fName = isMale ? firstNamesM[r.nextInt(firstNamesM.length)]
                    : firstNamesF[r.nextInt(firstNamesF.length)];
            String lName = lastNames[r.nextInt(lastNames.length)];
            if (!isMale && lName.endsWith("i")) {
                lName = lName.substring(0, lName.length() - 1) + "a";
            }
            Teacher t = new Teacher();
            t.setFirstName(fName);
            t.setLastName(lName);
            t.setEmail((fName.substring(0, 1) + "." + lName + tCount + "@szkola.pl").toLowerCase().replace(" ", ""));
            teachers.add(teacherRepository.save(t));
            tCount++;
        }

        List<Subject> subjects = new ArrayList<>();

        addSubject(subjects, "Historia", teachers.get(0));
        addSubject(subjects, "WOS", teachers.get(0));

        addSubject(subjects, "Matematyka", teachers.get(1));
        addSubject(subjects, "Informatyka", teachers.get(1));

        addSubject(subjects, "Matematyka", teachers.get(2));
        addSubject(subjects, "Fizyka", teachers.get(2));

        addSubject(subjects, "J. polski", teachers.get(3));
        addSubject(subjects, "Historia", teachers.get(3));

        String[] standardSubjects = { "Biologia", "Chemia", "Geografia", "Wychowanie fizyczne", "J. angielski",
                "J. niemiecki", "Muzyka", "Plastyka", "Technika", "EDB", "Religia" };
        for (int i = 4; i < 30; i++) {
            String subjName = standardSubjects[i % standardSubjects.length];
            addSubject(subjects, subjName, teachers.get(i));
        }

        List<SchoolClass> schoolClasses = new ArrayList<>();
        String[] classNames = { "1A", "1B", "1C", "2A", "2B", "2C", "3A", "3B", "3C", "4A" };
        for (int i = 0; i < classNames.length; i++) {
            SchoolClass c = new SchoolClass();
            c.setName(classNames[i]);
            c.setTeacher(teachers.get(i % teachers.size()));
            schoolClasses.add(schoolClassRepository.save(c));
        }

        List<Room> rooms = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Room room = new Room();
            room.setName("Sala " + (100 + i));
            rooms.add(room);
        }
        roomRepository.saveAll(rooms);

        List<Student> students = new ArrayList<>();
        for (SchoolClass sc : schoolClasses) {
            for (int i = 0; i < 30; i++) {
                boolean isMale = r.nextBoolean();
                String fName = isMale ? firstNamesM[r.nextInt(firstNamesM.length)]
                        : firstNamesF[r.nextInt(firstNamesF.length)];
                String lName = lastNames[r.nextInt(lastNames.length)];
                if (!isMale && lName.endsWith("i")) {
                    lName = lName.substring(0, lName.length() - 1) + "a";
                }
                Student st = new Student();
                st.setFirstName(fName);
                st.setLastName(lName);
                st.setSchoolClass(sc);
                String unq = String.valueOf(r.nextInt(100) + students.size());
                st.setEmail((fName.substring(0, 1) + "." + lName + unq + "@uczen.pl").toLowerCase().replace(" ", ""));
                students.add(studentRepository.save(st));
            }
        }

        java.util.Map<String, List<Subject>> groupedSubjects = new java.util.HashMap<>();
        for (Subject s : subjects) {
            groupedSubjects.computeIfAbsent(s.getName(), k -> new ArrayList<>()).add(s);
        }

        String[] gradesScale = { "1", "1+", "2-", "2", "2+", "3-", "3", "3+", "4-", "4", "4+", "5-", "5", "5+", "6-",
                "6" };
        for (Student st : students) {
            List<Grade> studentGrades = new ArrayList<>();
            List<Subject> studentEnrolledSubjects = new ArrayList<>();
            for (List<Subject> variants : groupedSubjects.values()) {
                studentEnrolledSubjects.add(variants.get(r.nextInt(variants.size())));
            }

            for (Subject sub : studentEnrolledSubjects) {
                int gradesCount = 10 + r.nextInt(4);
                for (int k = 0; k < gradesCount; k++) {
                    String val = gradesScale[r.nextInt(gradesScale.length)];
                    LocalDate date = LocalDate.of(2026, 1 + r.nextInt(6), 1 + r.nextInt(28));
                    Grade grade = new Grade();
                    grade.setStudent(st);
                    grade.setSubject(sub);
                    grade.setValue(val);
                    grade.setDate(date);
                    studentGrades.add(grade);
                }
            }
            gradeRepository.saveAll(studentGrades);

            List<Attendance> studentAtt = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                Attendance a = new Attendance();
                a.setStudent(st);
                a.setSubject(studentEnrolledSubjects.get(r.nextInt(studentEnrolledSubjects.size())));
                a.setDate(LocalDate.of(2026, 1 + r.nextInt(6), 1 + r.nextInt(28)));
                int statusRand = r.nextInt(100);
                if (statusRand < 75) a.setStatus(AttendanceStatus.PRESENT);
                else if (statusRand < 85) a.setStatus(AttendanceStatus.LATE);
                else a.setStatus(AttendanceStatus.ABSENT);
                studentAtt.add(a);
            }
            attendanceRepository.saveAll(studentAtt);
        }

        System.out.println("\n============ DATA LOADER V3 (OGROMNY) ============");
        System.out.println("Nauczyciele docelowi:    " + teacherRepository.count() + " / 30");
        System.out.println("Klasy uczniowskie:       " + schoolClassRepository.count());
        System.out.println("Karty Przedmiotow:       " + subjectRepository.count());
        System.out.println("Laczna baza Uczniow:     " + studentRepository.count() + " ucz.");
        System.out.println("Wszystkie Zarejestrowane Sale lekcyjne: " + roomRepository.count());
        System.out.println("Zlogowana baza historii wpisow Obc/Spl.: " + attendanceRepository.count());
        System.out.println("Wszystkie Oceny systemu: " + gradeRepository.count());
        System.out.println("==================================================\n");
    }

    private void addSubject(List<Subject> list, String name, Teacher teacher) {
        Subject s = new Subject();
        s.setName(name);
        s.setTeacher(teacher);
        list.add(subjectRepository.save(s));
    }
}
