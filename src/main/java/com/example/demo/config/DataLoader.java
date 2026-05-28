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

        String[] firstNamesM = { "Jan", "Piotr", "Tomasz", "Krzysztof", "Michal", "Maciej", "Dawid", "Kamil",
                "Filip", "Szymon", "Marek", "Pawel", "Robert", "Grzegorz", "Lukasz", "Marcin" };
        String[] firstNamesF = { "Anna", "Maria", "Katarzyna", "Malgorzata", "Agnieszka", "Barbara", "Ewa",
                "Krystyna", "Joanna", "Monika", "Izabela", "Magdalena", "Beata", "Dorota", "Halina" };
        String[] lastNames = { "Nowak", "Kowalski", "Wisniewski", "Wojcik", "Kowalczyk", "Kaminski",
                "Lewandowski", "Zielinski", "Szymanski", "Wozniak", "Dabrowski", "Kozlowski", "Jankowski",
                "Mazur", "Krawczyk", "Kaczmarek", "Piotrowski", "Grabowski", "Zajac", "Pawlowski",
                "Michalski", "Nowicki", "Adamczyk", "Dudek", "Zwiec", "Wieczorek", "Mroz", "Stepien",
                "Olszewski", "Jaworski", "Maliszewski", "Gajewski" };

        // ============================================================
        // 1. NAUCZYCIELE (30)
        //    Każdy ma konto ROLE_TEACHER: nauczyciel1 … nauczyciel30
        // ============================================================
        List<Teacher> teachers = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
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
            t.setEmail((fName.charAt(0) + "." + lName + i + "@szkola.pl").toLowerCase());
            teachers.add(teacherRepository.save(t));

            AppUser u = new AppUser();
            u.setUsername("nauczyciel" + (i + 1));
            u.setPassword(passwordEncoder.encode("haslo123"));
            u.setRole(Role.ROLE_TEACHER);
            u.setTeacher(t);
            appUserRepository.save(u);
        }

        // ============================================================
        // 2. KLASY (10): 1A, 1B, 1C, 2A, 2B, 2C, 3A, 3B, 3C, 4A
        // ============================================================
        List<SchoolClass> schoolClasses = new ArrayList<>();
        String[] classNames = { "1A", "1B", "1C", "2A", "2B", "2C", "3A", "3B", "3C", "4A" };
        for (int i = 0; i < classNames.length; i++) {
            SchoolClass c = new SchoolClass();
            c.setName(classNames[i]);
            c.setTeacher(teachers.get(i % teachers.size()));
            schoolClasses.add(schoolClassRepository.save(c));
        }

        // ============================================================
        // 3. SALE LEKCYJNE (101–125)
        // ============================================================
        List<Room> rooms = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Room room = new Room();
            room.setName(String.valueOf(100 + i));
            rooms.add(room);
        }
        roomRepository.saveAll(rooms);

        // ============================================================
        // 4. PRZEDMIOTY — po jednym egzemplarzu na (klasa × przedmiot)
        //
        //    Mamy 10 klas i 10 przedmiotów → 100 rekordów Subject.
        //    Każda klasa ma INNEGO nauczyciela dla tego samego przedmiotu.
        //
        //    Przypisanie nauczyciela:
        //      teacherIdx = (classIdx + subjectIdx * 3) % 30
        //
        //    Dowód unikalności w ramach klasy:
        //      Dla stałego ci, dwa subjectIdx si1 ≠ si2 z [0,9] dają
        //      (ci + si1*3) ≢ (ci + si2*3) mod 30  ⟺  si1 ≢ si2 mod 10 — spełnione.
        //    ⟹ Żadna klasa nie ma dwóch tych samych nauczycieli.
        //
        //    Godziny/tydzień (suma = 30 = 5 dni × 6 godzin):
        //      Matematyka 4, J.polski 4, J.angielski 3, Historia 3,
        //      Biologia 2, Chemia 2, Fizyka 2, Geografia 2, Informatyka 3, WF 5
        // ============================================================
        final String[] SUBJECT_NAMES = {
            "Matematyka",           // si=0  → 4 godz/tydz
            "J. polski",            // si=1  → 4 godz/tydz
            "J. angielski",         // si=2  → 3 godz/tydz
            "Historia",             // si=3  → 3 godz/tydz
            "Biologia",             // si=4  → 2 godz/tydz
            "Chemia",               // si=5  → 2 godz/tydz
            "Fizyka",               // si=6  → 2 godz/tydz
            "Geografia",            // si=7  → 2 godz/tydz
            "Informatyka",          // si=8  → 3 godz/tydz
            "Wychowanie fizyczne"   // si=9  → 5 godz/tydz
        };
        // Suma: 4+4+3+3+2+2+2+2+3+5 = 30 ✓
        final int[] LESSONS_PER_WEEK = { 4, 4, 3, 3, 2, 2, 2, 2, 3, 5 };

        int nC = schoolClasses.size(); // 10
        int nS = SUBJECT_NAMES.length; // 10

        // classSubjects[ci][si] = Subject przypisany do klasy ci dla przedmiotu si
        Subject[][] classSubjects = new Subject[nC][nS];
        for (int ci = 0; ci < nC; ci++) {
            for (int si = 0; si < nS; si++) {
                int teacherIdx = (ci + si * 3) % 30;
                Subject sub = new Subject();
                sub.setName(SUBJECT_NAMES[si]);
                sub.setTeacher(teachers.get(teacherIdx));
                classSubjects[ci][si] = subjectRepository.save(sub);
            }
        }

        // ============================================================
        // 5. UCZNIOWIE (10 klas × 30 = 300)
        //    Oceny i obecności TYLKO z przedmiotów własnej klasy
        // ============================================================
        String[] gradesScale = { "1", "1+", "2-", "2", "2+", "3-", "3", "3+",
                                  "4-", "4", "4+", "5-", "5", "5+", "6-", "6" };
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
                st.setEmail((fName.charAt(0) + "." + lName + globalIdx + "@uczen.pl").toLowerCase());
                studentRepository.save(st);

                AppUser su = new AppUser();
                su.setUsername("uczen" + (globalIdx + 1));
                su.setPassword(passwordEncoder.encode("haslo123"));
                su.setRole(Role.ROLE_STUDENT);
                su.setStudent(st);
                appUserRepository.save(su);
                globalIdx++;

                // Oceny z każdego przedmiotu tej klasy
                List<Grade> grades = new ArrayList<>();
                for (int si = 0; si < nS; si++) {
                    Subject sub = classSubjects[ci][si];
                    int cnt = 5 + r.nextInt(6); // 5–10 ocen
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

                // Obecności
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
        // 6. SPRAWDZIANY (2 per klasa)
        // ============================================================
        for (int ci = 0; ci < nC; ci++) {
            SchoolClass sc = schoolClasses.get(ci);
            for (int e = 0; e < 2; e++) {
                Subject sub = classSubjects[ci][r.nextInt(nS)];
                Exam exam = new Exam();
                exam.setTitle(e == 0
                        ? "Sprawdzian z rozdziału " + (r.nextInt(5) + 1)
                        : "Kartkówka – wzory i definicje");
                exam.setDescription(e == 0
                        ? "Proszę powtórzyć materiał z podręcznika."
                        : "Wzory skróconego mnożenia i definicje.");
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
        //    Śledzenie zajętości:
        //      teacherSlotBusy : "teacherId_dzien_slot"  → nauczyciel wolny
        //      classSlotBusy   : "classId_dzien_slot"    → klasa wolna
        //
        //    Algorytm: dla każdej lekcji klasy iterujemy przez wszystkie
        //    dostępne sloty i bierzemy pierwszy wolny dla obu stron.
        // ============================================================
        LocalTime[] startTimes = {
            LocalTime.of( 8,  0), LocalTime.of( 8, 55), LocalTime.of( 9, 50),
            LocalTime.of(10, 45), LocalTime.of(11, 55), LocalTime.of(12, 50)
        };
        LocalTime[] endTimes = {
            LocalTime.of( 8, 45), LocalTime.of( 9, 40), LocalTime.of(10, 35),
            LocalTime.of(11, 30), LocalTime.of(12, 40), LocalTime.of(13, 35)
        };
        DayOfWeek[] schoolDays = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        };

        // Wszystkie sloty (d=dzień 0-4, s=godzina 0-5)
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

            // Lista lekcji tej klasy z powtórzeniami (30 pozycji łącznie)
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
        System.out.println("Nauczyciele :  " + teacherRepository.count()   + "  (30)");
        System.out.println("Klasy       :  " + schoolClassRepository.count()+ "  (10)");
        System.out.println("Przedmioty  :  " + subjectRepository.count()   + "  (100 = 10 klas × 10 przedm.)");
        System.out.println("Uczniowie   :  " + studentRepository.count()   + "  (300)");
        System.out.println("Sale        :  " + roomRepository.count()      + "  (25)");
        System.out.println("Oceny       :  " + gradeRepository.count());
        System.out.println("Obecnosci   :  " + attendanceRepository.count());
        System.out.println("Plan zajec  :  " + scheduleRepository.count()  + "  (powinno byc 300)");
        System.out.println("=============================================================\n");
    }
}
