package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

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
    private final AppUserRepository appUserRepository;
    private final ExamRepository examRepository;
    private final ScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        // ============================================================
        // Konta systemowe — zawsze tworzone jeśli brak
        // ============================================================
        if (appUserRepository.findByUsername("admin").isEmpty()) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setRole(Role.ROLE_ADMIN);
            appUserRepository.save(admin);
        }
        if (appUserRepository.findByUsername("dyrektor").isEmpty()) {
            AppUser director = new AppUser();
            director.setUsername("dyrektor");
            director.setPassword(passwordEncoder.encode("dyrektor123"));
            director.setRole(Role.ROLE_DIRECTOR);
            appUserRepository.save(director);
        }
        if (appUserRepository.findByUsername("sekretariat").isEmpty()) {
            AppUser secretary = new AppUser();
            secretary.setUsername("sekretariat");
            secretary.setPassword(passwordEncoder.encode("sekretariat123"));
            secretary.setRole(Role.ROLE_SECRETARY);
            appUserRepository.save(secretary);
        }

        // Jeśli dane już istnieją — pomiń
        if (studentRepository.count() > 0) {
            return;
        }

        Random r = new Random(42);

        // ============================================================
        // Imiona i nazwiska (pomocnicze tablice)
        // ============================================================
        String[] firstNamesM = {
            "Jan", "Piotr", "Tomasz", "Krzysztof", "Michał", "Maciej", "Dawid", "Kamil",
            "Filip", "Szymon", "Marek", "Paweł", "Robert", "Grzegorz", "Łukasz", "Marcin",
            "Bartosz", "Artur", "Wojciech", "Jakub", "Mateusz", "Przemysław", "Sebastian", "Adam"
        };
        String[] firstNamesF = {
            "Anna", "Maria", "Katarzyna", "Małgorzata", "Agnieszka", "Barbara", "Ewa",
            "Krystyna", "Joanna", "Monika", "Izabela", "Magdalena", "Beata", "Dorota",
            "Halina", "Natalia", "Karolina", "Paulina", "Weronika", "Aleksandra", "Zuzanna", "Patrycja"
        };
        String[] lastNames = {
            "Nowak", "Kowalski", "Wiśniewski", "Wójcik", "Kowalczyk", "Kamiński",
            "Lewandowski", "Zieliński", "Szymański", "Woźniak", "Dąbrowski", "Kozłowski",
            "Jankowski", "Mazur", "Krawczyk", "Kaczmarek", "Piotrowski", "Grabowski",
            "Zając", "Pawłowski", "Michalski", "Nowicki", "Adamczyk", "Dudek",
            "Zawadzki", "Wieczorek", "Mróz", "Stępień", "Olszewski", "Jaworski"
        };

        // ============================================================
        // 1. PRZEDMIOTY — 14 przedmiotów, każdy z 1 dedykowanym nauczycielem
        //    (nauczyciel prowadzi ten przedmiot we wszystkich 5 klasach)
        // ============================================================
        final String[] SUBJECT_NAMES = {
            "Biologia",                     // 0 → 2 godz/tydz
            "Chemia",                       // 1 → 2 godz/tydz
            "Edukacja dla bezpieczeństwa",  // 2 → 1 godz/tydz
            "Fizyka",                       // 3 → 2 godz/tydz
            "Geografia",                    // 4 → 2 godz/tydz
            "Historia",                     // 5 → 3 godz/tydz
            "Informatyka",                  // 6 → 2 godz/tydz
            "Język polski",                 // 7 → 4 godz/tydz
            "Język angielski",              // 8 → 3 godz/tydz
            "Język hiszpański",             // 9 → 2 godz/tydz
            "Matematyka",                   // 10 → 4 godz/tydz
            "Plastyka",                     // 11 → 1 godz/tydz
            "Muzyka",                       // 12 → 1 godz/tydz
            "Wychowanie fizyczne"           // 13 → 3 godz/tydz
        };
        // Suma: 2+2+1+2+2+3+2+4+3+2+4+1+1+3 = 32 godz/tydz (5 dni × 6-7 godzin)
        final int[] LESSONS_PER_WEEK = { 2, 2, 1, 2, 2, 3, 2, 4, 3, 2, 4, 1, 1, 3 };

        int nS = SUBJECT_NAMES.length; // 14

        // Tworzenie 14 nauczycieli — po jednym na przedmiot
        List<Teacher> subjectTeachers = new ArrayList<>();
        for (int si = 0; si < nS; si++) {
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
            t.setEmail((fName.charAt(0) + "." + lName + si + "@szkola.pl").toLowerCase()
                    .replace("ą","a").replace("ę","e").replace("ó","o").replace("ś","s")
                    .replace("ł","l").replace("ź","z").replace("ż","z").replace("ć","c")
                    .replace("ń","n").replace("ź","z"));
            subjectTeachers.add(teacherRepository.save(t));

            AppUser u = new AppUser();
            u.setUsername("nauczyciel" + (si + 1));
            u.setPassword(passwordEncoder.encode("haslo123"));
            u.setRole(Role.ROLE_TEACHER);
            u.setTeacher(t);
            appUserRepository.save(u);
        }

        // ============================================================
        // 2. KLASY (5): 1A, 1B, 2A, 2B, 3A
        //    Wychowawcą klasy jest nauczyciel j.polskiego (si=7)
        // ============================================================
        List<SchoolClass> schoolClasses = new ArrayList<>();
        String[] classNames = { "1A", "1B", "2A", "2B", "3A" };
        // Wychowawcy: kolejni nauczyciele (biologia, chemia, historia, j.angielski, matematyka)
        int[] homeroomTeacherIdx = { 0, 1, 5, 8, 10 };
        for (int ci = 0; ci < classNames.length; ci++) {
            SchoolClass c = new SchoolClass();
            c.setName(classNames[ci]);
            c.setTeacher(subjectTeachers.get(homeroomTeacherIdx[ci]));
            schoolClasses.add(schoolClassRepository.save(c));
        }

        int nC = schoolClasses.size(); // 5

        // ============================================================
        // 3. SALE LEKCYJNE (101–120)
        // ============================================================
        List<Room> rooms = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            Room room = new Room();
            room.setName(String.valueOf(100 + i));
            rooms.add(room);
        }
        roomRepository.saveAll(rooms);

        // ============================================================
        // 4. PRZEDMIOTY w kontekście klas
        //    classSubjects[ci][si] = Subject dla klasy ci i przedmiotu si
        //    Każdy przedmiot (si) ma TEGO SAMEGO nauczyciela we wszystkich klasach.
        // ============================================================
        Subject[][] classSubjects = new Subject[nC][nS];
        for (int ci = 0; ci < nC; ci++) {
            for (int si = 0; si < nS; si++) {
                Subject sub = new Subject();
                sub.setName(SUBJECT_NAMES[si]);
                sub.setTeacher(subjectTeachers.get(si));
                classSubjects[ci][si] = subjectRepository.save(sub);
            }
        }

        // ============================================================
        // 5. UCZNIOWIE (5 klas × 30 = 150)
        //    Oceny i obecności z każdego przedmiotu klasy
        // ============================================================
        String[] gradesScale = {
            "1", "1+", "2-", "2", "2+", "3-", "3", "3+",
            "4-", "4", "4+", "5-", "5", "5+", "6-", "6"
        };
        int globalIdx = 0;

        for (int ci = 0; ci < nC; ci++) {
            SchoolClass sc = schoolClasses.get(ci);
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
                st.setEmail((fName.charAt(0) + "." + lName + globalIdx + "@uczen.pl").toLowerCase()
                        .replace("ą","a").replace("ę","e").replace("ó","o").replace("ś","s")
                        .replace("ł","l").replace("ź","z").replace("ż","z").replace("ć","c")
                        .replace("ń","n"));
                studentRepository.save(st);

                AppUser su = new AppUser();
                su.setUsername("uczen" + (globalIdx + 1));
                su.setPassword(passwordEncoder.encode("haslo123"));
                su.setRole(Role.ROLE_STUDENT);
                su.setStudent(st);
                appUserRepository.save(su);
                globalIdx++;

                // Oceny z każdego przedmiotu tej klasy (5–10 ocen)
                List<Grade> grades = new ArrayList<>();
                for (int si = 0; si < nS; si++) {
                    Subject sub = classSubjects[ci][si];
                    int cnt = 5 + r.nextInt(6);
                    for (int k = 0; k < cnt; k++) {
                        Grade g = new Grade();
                        g.setStudent(st);
                        g.setSubject(sub);
                        g.setValue(gradesScale[r.nextInt(gradesScale.length)]);
                        g.setDate(LocalDate.of(2026, 1 + r.nextInt(5), 1 + r.nextInt(28)));
                        grades.add(g);
                    }
                }
                gradeRepository.saveAll(grades);

                // Obecności (40 wpisów)
                List<Attendance> atts = new ArrayList<>();
                for (int k = 0; k < 40; k++) {
                    Attendance a = new Attendance();
                    a.setStudent(st);
                    a.setSubject(classSubjects[ci][r.nextInt(nS)]);
                    a.setDate(LocalDate.of(2026, 1 + r.nextInt(5), 1 + r.nextInt(28)));
                    int rn = r.nextInt(100);
                    a.setStatus(rn < 75 ? AttendanceStatus.PRESENT
                              : rn < 85 ? AttendanceStatus.LATE
                                        : AttendanceStatus.ABSENT);
                    atts.add(a);
                }
                attendanceRepository.saveAll(atts);
            }
        }

        // ============================================================
        // 6. SPRAWDZIANY (3 per klasa)
        // ============================================================
        for (int ci = 0; ci < nC; ci++) {
            SchoolClass sc = schoolClasses.get(ci);
            for (int e = 0; e < 3; e++) {
                Subject sub = classSubjects[ci][r.nextInt(nS)];
                Exam exam = new Exam();
                String[] titles = {
                    "Sprawdzian z rozdziału " + (r.nextInt(5) + 1),
                    "Kartkówka – wzory i definicje",
                    "Powtórzenie materiału semestralnego"
                };
                exam.setTitle(titles[e]);
                exam.setDescription("Proszę powtórzyć materiał z podręcznika – rozdziały 1-" + (e + 3) + ".");
                exam.setDate(LocalDate.now().plusDays(r.nextInt(21) + 1));
                exam.setSchoolClass(sc);
                exam.setSubject(sub);
                exam.setTeacher(sub.getTeacher());
                examRepository.save(exam);
            }
        }

        // ============================================================
        // 7. PLAN LEKCJI — bez konfliktów
        //
        //    Godziny lekcyjne: 8 slotów dziennie × 5 dni = 40 slotów/tydz na klasę.
        //    Łączna liczba lekcji dla klasy: 32 (suma LESSONS_PER_WEEK).
        //    Śledzenie zajętości:
        //      teacherSlotBusy : "teacherId_dzien_slot"
        //      classSlotBusy   : "classId_dzien_slot"
        // ============================================================
        LocalTime[] startTimes = {
            LocalTime.of( 8,  0), LocalTime.of( 8, 55), LocalTime.of( 9, 50),
            LocalTime.of(10, 45), LocalTime.of(11, 55), LocalTime.of(12, 50),
            LocalTime.of(13, 45), LocalTime.of(14, 40)
        };
        LocalTime[] endTimes = {
            LocalTime.of( 8, 45), LocalTime.of( 9, 40), LocalTime.of(10, 35),
            LocalTime.of(11, 30), LocalTime.of(12, 40), LocalTime.of(13, 35),
            LocalTime.of(14, 30), LocalTime.of(15, 25)
        };
        DayOfWeek[] schoolDays = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        };

        List<int[]> allSlots = new ArrayList<>();
        for (int d = 0; d < schoolDays.length; d++) {
            for (int s = 0; s < startTimes.length; s++) {
                allSlots.add(new int[]{ d, s });
            }
        }

        Set<String> teacherSlotBusy = new HashSet<>();
        Set<String> classSlotBusy   = new HashSet<>();
        List<Schedule> schedulesToSave = new ArrayList<>();

        for (int ci = 0; ci < nC; ci++) {
            SchoolClass sc = schoolClasses.get(ci);
            Long classId = sc.getId();

            // Lista lekcji tej klasy z powtórzeniami (32 pozycji łącznie)
            List<Subject> lessonPlan = new ArrayList<>();
            for (int si = 0; si < nS; si++) {
                for (int rep = 0; rep < LESSONS_PER_WEEK[si]; rep++) {
                    lessonPlan.add(classSubjects[ci][si]);
                }
            }
            // Mieszamy, żeby przedmioty były rozłożone przez cały tydzień
            Collections.shuffle(lessonPlan, new Random(ci * 97L + 13));

            for (Subject sub : lessonPlan) {
                Long teacherId = sub.getTeacher().getId();
                boolean assigned = false;

                for (int[] slot : allSlots) {
                    int d = slot[0], s = slot[1];
                    String ck = classId   + "_" + d + "_" + s;
                    String tk = teacherId + "_" + d + "_" + s;

                    if (!classSlotBusy.contains(ck) && !teacherSlotBusy.contains(tk)) {
                        Schedule schedule = new Schedule();
                        schedule.setSchoolClass(sc);
                        schedule.setSubject(sub);
                        schedule.setRoom(rooms.get(r.nextInt(rooms.size())));
                        schedule.setDayOfWeek(schoolDays[d]);
                        schedule.setStartTime(startTimes[s]);
                        schedule.setEndTime(endTimes[s]);

                        classSlotBusy.add(ck);
                        teacherSlotBusy.add(tk);
                        schedulesToSave.add(schedule);
                        assigned = true;
                        break;
                    }
                }

                if (!assigned) {
                    System.err.println("[PLAN] Nie mozna przypisac slotu: "
                        + sub.getName() + " | nauczyciel #" + teacherId
                        + " | klasa " + sc.getName());
                }
            }
        }

        scheduleRepository.saveAll(schedulesToSave);

        System.out.println("\n============ DZIENNIK SZKOLNY — DANE ZALADOWANE ============");
        System.out.println("Nauczyciele :  " + teacherRepository.count()    + "  (14)");
        System.out.println("Klasy       :  " + schoolClassRepository.count() + "  (5)");
        System.out.println("Przedmioty  :  " + subjectRepository.count()    + "  (70 = 5 klas × 14 przedm.)");
        System.out.println("Uczniowie   :  " + studentRepository.count()    + "  (150 = 5 klas × 30)");
        System.out.println("Sale        :  " + roomRepository.count()       + "  (20)");
        System.out.println("Oceny       :  " + gradeRepository.count());
        System.out.println("Obecnosci   :  " + attendanceRepository.count());
        System.out.println("Plan zajec  :  " + scheduleRepository.count()   + "  (powinno byc 160 = 5 klas × 32 godz)");
        System.out.println("=============================================================\n");
    }
}
