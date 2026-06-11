package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final StudentRepository    studentRepository;
    private final TeacherRepository    teacherRepository;
    private final SubjectRepository    subjectRepository;
    private final GradeRepository      gradeRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final RoomRepository       roomRepository;
    private final AttendanceRepository attendanceRepository;
    private final AppUserRepository    appUserRepository;
    private final ExamRepository       examRepository;
    private final ScheduleRepository   scheduleRepository;
    private final PasswordEncoder      passwordEncoder;

    @Override
    public void run(String... args) {

        //Konta systemowe
        createSystemAccount("admin",       "admin",          Role.ROLE_ADMIN,      null, null);
        createSystemAccount("dyrektor",    "dyrektor123",    Role.ROLE_DIRECTOR,   null, null);
        createSystemAccount("sekretariat", "sekretariat123", Role.ROLE_SECRETARY,  null, null);

        if (studentRepository.count() > 0) return;   // dane już załadowane

        Random rng = new Random(20250901L);

        //Dni szkolne roku 2025/2026
        Set<LocalDate>  holidays   = buildHolidays();
        List<LocalDate> schoolDays = buildSchoolDays(
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2026, 6, 26),
                holidays);

        LocalDate today = LocalDate.now();

        //Tablice imion i nazwisk
        String[] maleNames = {
            "Jan", "Piotr", "Tomasz", "Michał", "Maciej", "Kamil", "Filip", "Szymon",
            "Jakub", "Mateusz", "Bartosz", "Wojciech", "Adam", "Paweł", "Łukasz",
            "Marcin", "Grzegorz", "Krzysztof", "Dawid", "Sebastian", "Przemysław",
            "Rafał", "Marek", "Damian", "Artur"
        };
        String[] femaleNames = {
            "Anna", "Maria", "Katarzyna", "Agnieszka", "Barbara", "Ewa", "Joanna",
            "Monika", "Izabela", "Magdalena", "Beata", "Dorota", "Natalia", "Karolina",
            "Paulina", "Weronika", "Aleksandra", "Zuzanna", "Patrycja", "Halina",
            "Elżbieta", "Małgorzata", "Klaudia", "Justyna", "Sylwia"
        };
        String[] lastNamesM = {
            "Nowak", "Kowalski", "Wiśniewski", "Wójcik", "Kowalczyk", "Kamiński",
            "Lewandowski", "Zieliński", "Szymański", "Woźniak", "Dąbrowski", "Kozłowski",
            "Jankowski", "Mazur", "Krawczyk", "Kaczmarek", "Piotrowski", "Grabowski",
            "Zając", "Pawłowski", "Michalski", "Nowicki", "Adamczyk", "Dudek",
            "Zawadzki", "Wieczorek", "Mróz", "Stępień", "Olszewski", "Jaworski",
            "Wróbel", "Majewski", "Janik", "Wojciechowski", "Kwiatkowski"
        };
        String[] lastNamesF = {
            "Nowak", "Kowalska", "Wiśniewska", "Wójcik", "Kowalczyk", "Kamińska",
            "Lewandowska", "Zielińska", "Szymańska", "Woźniak", "Dąbrowska", "Kozłowska",
            "Jankowska", "Mazur", "Krawczyk", "Kaczmarek", "Piotrowska", "Grabowska",
            "Zając", "Pawłowska", "Michalska", "Nowicka", "Adamczyk", "Dudek",
            "Zawadzka", "Wieczorek", "Mróz", "Stępień", "Olszewska", "Jaworska",
            "Wróbel", "Majewska", "Janik", "Wojciechowska", "Kwiatkowska"
        };

        String[][] teacherData = {
            {"Maria",     "Kowalska",       "m.kowalska@sp1.lublin.pl"},
            {"Anna",      "Wiśniewska",     "a.wisniewska@sp1.lublin.pl"},
            {"Tomasz",    "Dąbrowski",      "t.dabrowski@sp1.lublin.pl"},
            {"Elżbieta",  "Malinowska",     "e.malinowska@sp1.lublin.pl"},
            {"Piotr",     "Nowak",          "p.nowak@sp1.lublin.pl"},
            {"Katarzyna", "Zielińska",      "k.zielinska@sp1.lublin.pl"},
            {"Marek",     "Wiśniewski",     "ma.wisniewski@sp1.lublin.pl"},
            {"Joanna",    "Wojciechowska",  "j.wojciechowska@sp1.lublin.pl"},
            {"Rafał",     "Kamiński",       "r.kaminski@sp1.lublin.pl"},
            {"Beata",     "Lewandowska",    "b.lewandowska@sp1.lublin.pl"},
            {"Michał",    "Mazur",          "mi.mazur@sp1.lublin.pl"},
            {"Agnieszka", "Szymańska",      "a.szymanska@sp1.lublin.pl"},
            {"Robert",    "Krawczyk",       "r.krawczyk@sp1.lublin.pl"},
            {"Izabela",   "Woźniak",        "i.wozniak@sp1.lublin.pl"},
            {"Łukasz",    "Grabowski",      "l.grabowski@sp1.lublin.pl"},
            {"Monika",    "Michalska",      "mo.michalska@sp1.lublin.pl"},
            {"Sebastian", "Jankowski",      "s.jankowski@sp1.lublin.pl"},
            {"Krzysztof", "Adamczyk",       "k.adamczyk@sp1.lublin.pl"},
            {"Paulina",   "Zawadzka",       "p.zawadzka@sp1.lublin.pl"},
            {"Halina",    "Wieczorek",      "h.wieczorek@sp1.lublin.pl"},
        };

        Teacher[] teachers = new Teacher[20];
        for (int i = 0; i < 20; i++) {
            Teacher t = new Teacher();
            t.setFirstName(teacherData[i][0]);
            t.setLastName(teacherData[i][1]);
            t.setEmail(teacherData[i][2]);
            teachers[i] = teacherRepository.save(t);
            createSystemAccount("nauczyciel" + (i + 1), "haslo123", Role.ROLE_TEACHER, null, teachers[i]);
        }

        // 2. KLASY (10)  –  wychowawcy: indeksy z tablicy homeroomIdx
        String[] classNames  = {"4A","4B","5A","5B","6A","6B","7A","7B","8A","8B"};
        int[]    homeroomIdx = {  0,   1,   4,   5,   7,   8,   2,   6,   3,   9 };

        SchoolClass[] classes = new SchoolClass[10];
        for (int i = 0; i < 10; i++) {
            SchoolClass c = new SchoolClass();
            c.setName(classNames[i]);
            c.setTeacher(teachers[homeroomIdx[i]]);
            classes[i] = schoolClassRepository.save(c);
        }

        // 3. SALE LEKCYJNE (25)
        List<Room> rooms = new ArrayList<>();
        String[] roomNames = {
            "101", "102", "103", "104", "105",
            "106", "107", "108", "109", "110",
            "201", "202", "203", "204", "205",
            "206", "207", "208",
            "Sala gimnastyczna", "Mała sala gimnastyczna",
            "Pracownia informatyczna", "Pracownia chemiczna",
            "Pracownia fizyczna", "Sala artystyczna", "Biblioteka"
        };
        for (String name : roomNames) {
            Room room = new Room();
            room.setName(name);
            rooms.add(roomRepository.save(room));
        }

        // Specjalne sale do konkretnych przedmiotów
        Room salaGim      = rooms.get(18);
        Room malaSalaGim  = rooms.get(19);
        Room pracowniaIT  = rooms.get(20);
        Room pracowniaChemia = rooms.get(21);
        Room pracowniaFiz = rooms.get(22);
        Room salaArt      = rooms.get(23);

        // 4. DEFINICJE PRZEDMIOTÓW PER POZIOM KLASY
        //  Klasy 4–6 (idx 0–5): 11 przedmiotów, 26 h/tydzień
        //  Klasy 7   (idx 6–7): 13 przedmiotów, 32 h/tydzień
        //  Klasy 8   (idx 8–9): 13 przedmiotów, 33 h/tydzień

        // Klasy 4–6
        String[] s46 = {
            "Język polski", "Matematyka", "Język angielski", "Historia",
            "Przyroda", "Informatyka", "Plastyka", "Muzyka",
            "Wychowanie fizyczne", "Technika", "Religia"
        };
        int[] h46 = {5, 4, 3, 2, 2, 1, 1, 1, 4, 1, 2};   // h/tydzień
        // teacher index for each class (rows 0-5) x subject (cols 0-10)
        int[][] t46 = {
            {0, 4, 7, 9, 10, 14, 15, 16, 17, 16, 19},  // 4A
            {1, 4, 7, 9, 10, 14, 15, 16, 18, 16, 19},  // 4B
            {0, 4, 7, 9, 11, 14, 15, 16, 17, 16, 19},  // 5A
            {1, 5, 7, 9, 11, 14, 15, 16, 18, 16, 19},  // 5B
            {0, 5, 7, 9, 12, 14, 15, 16, 17, 16, 19},  // 6A
            {1, 5, 8, 9, 12, 14, 15, 16, 18, 16, 19},  // 6B
        };

        // Klasy 7
        String[] s7 = {
            "Język polski", "Matematyka", "Język angielski", "Historia",
            "Biologia", "Chemia", "Fizyka", "Geografia",
            "Informatyka", "Plastyka", "Wychowanie fizyczne", "Technika", "Religia"
        };
        int[] h7 = {5, 4, 4, 2, 2, 2, 2, 2, 1, 1, 4, 1, 2};
        int[][] t7 = {
            {2, 6, 8, 9, 10, 11, 12, 13, 14, 15, 17, 16, 19},  // 7A
            {2, 6, 8, 9, 10, 11, 12, 13, 14, 15, 18, 16, 19},  // 7B
        };

        // Klasy 8
        String[] s8 = {
            "Język polski", "Matematyka", "Język angielski", "Historia",
            "Biologia", "Chemia", "Fizyka", "Geografia",
            "Informatyka", "Plastyka", "Wychowanie fizyczne",
            "Edukacja dla bezpieczeństwa", "Religia"
        };
        int[] h8 = {5, 4, 4, 3, 2, 2, 2, 2, 1, 1, 4, 1, 2};
        int[][] t8 = {
            {3, 6, 8, 9, 10, 11, 12, 13, 14, 15, 17, 19, 19},  // 8A
            {3, 6, 8, 9, 10, 11, 12, 13, 14, 15, 18, 19, 19},  // 8B
        };

        // Tworzenie encji Subject per klasa
        @SuppressWarnings("unchecked")
        List<Subject>[] classSubjects = new List[10];

        for (int ci = 0; ci < 6; ci++) {
            classSubjects[ci] = new ArrayList<>();
            for (int si = 0; si < s46.length; si++) {
                Subject sub = new Subject();
                sub.setName(s46[si]);
                sub.setTeacher(teachers[t46[ci][si]]);
                classSubjects[ci].add(subjectRepository.save(sub));
            }
        }
        for (int ci = 6; ci < 8; ci++) {
            classSubjects[ci] = new ArrayList<>();
            for (int si = 0; si < s7.length; si++) {
                Subject sub = new Subject();
                sub.setName(s7[si]);
                sub.setTeacher(teachers[t7[ci - 6][si]]);
                classSubjects[ci].add(subjectRepository.save(sub));
            }
        }
        for (int ci = 8; ci < 10; ci++) {
            classSubjects[ci] = new ArrayList<>();
            for (int si = 0; si < s8.length; si++) {
                Subject sub = new Subject();
                sub.setName(s8[si]);
                sub.setTeacher(teachers[t8[ci - 8][si]]);
                classSubjects[ci].add(subjectRepository.save(sub));
            }
        }

        // 5. UCZNIOWIE — 25 per klasa = 250 łącznie
        // Skala ocen: 1–6 z plusami i minusami, ważona w kierunku 3-5
        String[] gradeValues = {
            "1", "2-", "2", "2+",
            "3-", "3", "3", "3+", "3+",
            "4-", "4", "4", "4+", "4+",
            "5-", "5", "5", "5+",
            "6-", "6"
        };

        int globalIdx = 0;

        for (int ci = 0; ci < 10; ci++) {
            SchoolClass sc = classes[ci];
            List<Subject> subjects = classSubjects[ci];

            for (int i = 0; i < 25; i++) {
                boolean isMale = rng.nextBoolean();
                String fName = isMale
                        ? maleNames[rng.nextInt(maleNames.length)]
                        : femaleNames[rng.nextInt(femaleNames.length)];
                String lName = isMale
                        ? lastNamesM[rng.nextInt(lastNamesM.length)]
                        : lastNamesF[rng.nextInt(lastNamesF.length)];

                Student st = new Student();
                st.setFirstName(fName);
                st.setLastName(lName);
                st.setSchoolClass(sc);
                st.setEmail(polish2ascii(
                        Character.toLowerCase(fName.charAt(0)) + "." + lName.toLowerCase()
                        + globalIdx + "@uczen.sp1lublin.pl"));
                studentRepository.save(st);

                createSystemAccount("uczen" + (globalIdx + 1), "haslo123",
                        Role.ROLE_STUDENT, st, null);
                globalIdx++;

                // Oceny: 4–8 na każdy przedmiot
                List<Grade> grades = new ArrayList<>();
                for (Subject sub : subjects) {
                    int cnt = 4 + rng.nextInt(5);
                    for (int k = 0; k < cnt; k++) {
                        Grade g = new Grade();
                        g.setStudent(st);
                        g.setSubject(sub);
                        g.setValue(gradeValues[rng.nextInt(gradeValues.length)]);
                        g.setDate(randomPastDay(schoolDays, today, rng));
                        grades.add(g);
                    }
                }
                gradeRepository.saveAll(grades);

                // Obecności: 40–65 wpisów
                List<Attendance> atts = new ArrayList<>();
                int attCount = 40 + rng.nextInt(26);
                for (int k = 0; k < attCount; k++) {
                    Attendance a = new Attendance();
                    a.setStudent(st);
                    a.setSubject(subjects.get(rng.nextInt(subjects.size())));
                    a.setDate(randomPastDay(schoolDays, today, rng));
                    int rn = rng.nextInt(100);
                    if (rn < 82) {
                        a.setStatus(AttendanceStatus.PRESENT);
                    } else if (rn < 92) {
                        a.setStatus(AttendanceStatus.LATE);
                    } else {
                        a.setStatus(AttendanceStatus.ABSENT);
                    }
                    a.setExcuseRequested(false);
                    atts.add(a);
                }
                attendanceRepository.saveAll(atts);
            }
        }

        // 6. PLAN ZAJĘĆ (tygodniowy, powtarzający się)
        //  Godziny lekcyjne (8 slotów dziennie):
        //   0: 08:00–08:45   1: 08:55–09:40   2: 09:50–10:35   3: 10:45–11:30   4: 11:55–12:40   5: 12:50–13:35   6: 13:45–14:30   7: 14:40–15:25
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
        DayOfWeek[] weekDays = {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        };

        // Mapowanie przedmiot - sala preferowana
        Map<String, Room> preferredRoom = new HashMap<>();
        preferredRoom.put("Wychowanie fizyczne",           salaGim);
        preferredRoom.put("Informatyka",                   pracowniaIT);
        preferredRoom.put("Chemia",                        pracowniaChemia);
        preferredRoom.put("Fizyka",                        pracowniaFiz);
        preferredRoom.put("Plastyka",                      salaArt);
        preferredRoom.put("Muzyka",                        salaArt);

        Set<String> teacherSlotBusy = new HashSet<>();
        Set<String> classSlotBusy   = new HashSet<>();
        List<Schedule> schedulesToSave = new ArrayList<>();

        int[][] allHoursPerClass = new int[10][];
        for (int ci = 0; ci < 6;  ci++) allHoursPerClass[ci] = h46;
        for (int ci = 6; ci < 8;  ci++) allHoursPerClass[ci] = h7;
        for (int ci = 8; ci < 10; ci++) allHoursPerClass[ci] = h8;

        // Bazowa lista slotów (40 = 5 dni × 8 slotów)
        List<int[]> baseSlots = new ArrayList<>();
        for (int d = 0; d < 5; d++)
            for (int s = 0; s < startTimes.length; s++)
                baseSlots.add(new int[]{d, s});

        for (int ci = 0; ci < 10; ci++) {
            SchoolClass sc = classes[ci];
            Long classId = sc.getId();
            List<Subject> subjects = classSubjects[ci];
            int[] hours = allHoursPerClass[ci];

            // Rozwijamy plan lekcji z powtórzeniami
            List<Subject> lessonPlan = new ArrayList<>();
            for (int si = 0; si < subjects.size(); si++)
                for (int rep = 0; rep < hours[si]; rep++)
                    lessonPlan.add(subjects.get(si));

            // Mieszamy kolejność przedmiotów w planie tej klasy
            Collections.shuffle(lessonPlan, new Random(ci * 137L + 2025L));

            for (Subject sub : lessonPlan) {
                Long teacherId = sub.getTeacher().getId();

                // Mieszamy sloty (różne ziarna per nauczyciel+klasa → równomierne rozmieszczenie)
                List<int[]> slots = new ArrayList<>(baseSlots);
                Collections.shuffle(slots, new Random(classId * 31L + teacherId * 17L + ci));

                boolean assigned = false;
                for (int[] slot : slots) {
                    int d = slot[0], s = slot[1];
                    String ck = classId   + "_" + d + "_" + s;
                    String tk = teacherId + "_" + d + "_" + s;

                    if (!classSlotBusy.contains(ck) && !teacherSlotBusy.contains(tk)) {
                        // Wybierz salę: preferowana lub losowa
                        Room room = preferredRoom.getOrDefault(sub.getName(),
                                rooms.get(rng.nextInt(18)));  // sale 101-208

                        // WF: co drugi slot w małej sali (podział na grupy)
                        if ("Wychowanie fizyczne".equals(sub.getName()) && rng.nextBoolean()) {
                            room = malaSalaGim;
                        }

                        Schedule schedule = new Schedule();
                        schedule.setSchoolClass(sc);
                        schedule.setSubject(sub);
                        schedule.setRoom(room);
                        schedule.setDayOfWeek(weekDays[d]);
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
                    System.err.printf("[PLAN] Brak slotu: %-30s | nauczyciel #%d | klasa %s%n",
                            sub.getName(), teacherId, sc.getName());
                }
            }
        }
        scheduleRepository.saveAll(schedulesToSave);

        // 7. SPRAWDZIANY I KARTKÓWKI (2–4 per klasa)
        List<LocalDate> futureDays = schoolDays.stream()
                .filter(d -> !d.isBefore(today))
                .limit(15)
                .collect(Collectors.toList());
        if (futureDays.isEmpty()) {
            // Rok szkolny dobiegł końca – używamy ostatnich dni
            int sz = schoolDays.size();
            futureDays = schoolDays.subList(Math.max(0, sz - 15), sz);
        }

        String[] examTemplates = {
            "Sprawdzian z rozdziału %d",
            "Kartkówka – definicje i wzory",
            "Powtórzenie materiału semestralnego",
            "Test z działu %d",
            "Sprawdzian semestralny",
            "Kartkówka – zadania",
            "Test wiedzy – dział %d"
        };

        for (int ci = 0; ci < 10; ci++) {
            SchoolClass sc = classes[ci];
            List<Subject> subjects = classSubjects[ci];
            int examCount = 2 + rng.nextInt(3);

            List<LocalDate> shuffledFuture = new ArrayList<>(futureDays);
            Collections.shuffle(shuffledFuture, new Random(ci * 53L + 7L));

            for (int e = 0; e < examCount && e < shuffledFuture.size(); e++) {
                Subject sub = pickExamSubject(subjects, rng);
                String template = examTemplates[rng.nextInt(examTemplates.length)];
                String title = template.contains("%d")
                        ? String.format(template, rng.nextInt(6) + 1)
                        : template;

                Exam exam = new Exam();
                exam.setTitle(title);
                exam.setDescription("Zakres materiału: rozdziały " + (e + 1) + "–" + (e + 3)
                        + ". Proszę powtórzyć notatki i zadania z ćwiczeń.");
                exam.setDate(shuffledFuture.get(e));
                exam.setSchoolClass(sc);
                exam.setSubject(sub);
                exam.setTeacher(sub.getTeacher());
                examRepository.save(exam);
            }
        }

        //Podsumowanie
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║   SP NR 1 IM. T. KOŚCIUSZKI W LUBLINIE — DANE ZAŁADOWANE    ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  Rok szkolny   : 2025/2026  (%d dni szkolnych)%n", schoolDays.size());
        System.out.printf( "║  Nauczyciele   : %d%n", teacherRepository.count());
        System.out.printf( "║  Klasy         : %d  (4A–8B)%n", schoolClassRepository.count());
        System.out.printf( "║  Uczniowie     : %d  (25 per klasa)%n", studentRepository.count());
        System.out.printf( "║  Sale          : %d%n", roomRepository.count());
        System.out.printf( "║  Przedmioty    : %d%n", subjectRepository.count());
        System.out.printf( "║  Oceny         : %d%n", gradeRepository.count());
        System.out.printf( "║  Obecności     : %d%n", attendanceRepository.count());
        System.out.printf( "║  Plan zajęć    : %d wpisów%n", scheduleRepository.count());
        System.out.printf( "║  Sprawdziany   : %d%n", examRepository.count());
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    // METODY POMOCNICZE

    private void createSystemAccount(String username, String rawPassword,
                                     Role role, Student student, Teacher teacher) {
        if (appUserRepository.findByUsername(username).isPresent()) return;
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setStudent(student);
        u.setTeacher(teacher);
        appUserRepository.save(u);
    }

    /** Buduje zbiór dni wolnych od nauki dla Lublina w roku szkolnym 2025/2026. */
    private Set<LocalDate> buildHolidays() {
        Set<LocalDate> h = new HashSet<>();

        // 11 XI 2025 – Dzień Niepodległości (wtorek)
        h.add(LocalDate.of(2025, 11, 11));

        // 23 XII 2025 – 2 I 2026 – przerwa świąteczna (Boże Narodzenie + Nowy Rok)
        addRange(h, LocalDate.of(2025, 12, 23), LocalDate.of(2026, 1,  2));

        // 6 I 2026 – Trzech Króli (wtorek – w okolicach przerwy bożonarodzeniowej)
        h.add(LocalDate.of(2026, 1, 6));

        // 2–15 II 2026 – ferie zimowe (lubelskie – turnus III)
        addRange(h, LocalDate.of(2026, 2,  2), LocalDate.of(2026, 2, 15));

        // 2–7 IV 2026 – wiosenna przerwa świąteczna (Wielkanoc: 5 IV 2026)
        // Wielki Czwartek–Wtorek po Wielkanocy
        addRange(h, LocalDate.of(2026, 4,  2), LocalDate.of(2026, 4,  7));

        // 1 V 2026 – Święto Pracy (piątek)
        h.add(LocalDate.of(2026, 5, 1));

        // 3 V 2026 – Święto Konstytucji (niedziela, bez wpływu, ale dodajemy)
        h.add(LocalDate.of(2026, 5, 3));

        // 4 VI 2026 – Boże Ciało (czwartek)
        h.add(LocalDate.of(2026, 6, 4));

        return h;
    }

    /** Dodaje wszystkie daty z zakresu [from, to] do zbioru. */
    private void addRange(Set<LocalDate> set, LocalDate from, LocalDate to) {
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1))
            set.add(d);
    }

    /** Zwraca listę dni szkolnych (Pon–Pt, bez świąt i przerw). */
    private List<LocalDate> buildSchoolDays(LocalDate from, LocalDate to,
                                            Set<LocalDate> holidays) {
        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY
                    && !holidays.contains(d)) {
                days.add(d);
            }
        }
        return days;
    }

    /** Zwraca losowy dzień szkolny sprzed dzisiaj. */
    private LocalDate randomPastDay(List<LocalDate> days, LocalDate today, Random rng) {
        // Szukamy ostatniego indeksu, gdzie data < today
        int upperBound = 0;
        for (LocalDate d : days) {
            if (d.isBefore(today)) upperBound++;
            else break;
        }
        if (upperBound == 0) return today.minusDays(1);
        return days.get(rng.nextInt(upperBound));
    }

    /**
     * Wybiera przedmiot nadający się na sprawdzian
     * (pomija WF, Religia, Technika, Muzyka, Plastyka, EDB).
     */
    private Subject pickExamSubject(List<Subject> subjects, Random rng) {
        Set<String> skip = Set.of("Wychowanie fizyczne", "Religia", "Technika",
                                  "Muzyka", "Plastyka", "Edukacja dla bezpieczeństwa");
        List<Subject> eligible = subjects.stream()
                .filter(s -> !skip.contains(s.getName()))
                .collect(Collectors.toList());
        if (eligible.isEmpty()) return subjects.get(rng.nextInt(subjects.size()));
        return eligible.get(rng.nextInt(eligible.size()));
    }

    /** Konwertuje polskie znaki na ASCII dla adresów e-mail. */
    private String polish2ascii(String s) {
        return s
            .replace("ą","a").replace("Ą","A").replace("ć","c").replace("Ć","C")
            .replace("ę","e").replace("Ę","E").replace("ł","l").replace("Ł","L")
            .replace("ń","n").replace("Ń","N").replace("ó","o").replace("Ó","O")
            .replace("ś","s").replace("Ś","S").replace("ź","z").replace("Ź","Z")
            .replace("ż","z").replace("Ż","Z");
    }
}
