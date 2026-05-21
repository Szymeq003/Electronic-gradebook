# Plan implementacyjny naprawy błędów – Dzienniczek Szkolny (część 2)

> Wygenerowany: 2026-05-21  
> Dotyczy błędów wykrytych podczas weryfikacji wdrożenia planu_naprawy_bledow.md  
> Status planu 1: wszystkie 7 punktów zaimplementowane poprawnie ✅

---

## STATUS POPRZEDNIEGO PLANU

| Błąd | Opis | Status |
|------|------|--------|
| 1 | Brak `th:action` → HTTP 403 przy dodawaniu dnia wolnego | ✅ Naprawiony |
| 2 | Rozjechane nagłówki formularza dni wolnych | ✅ Naprawiony |
| 3 | Przycisk „Powrót" używał `history.back()` | ✅ Naprawiony |
| 4 | Dni tygodnia sortowane alfabetycznie zamiast Pn→Pt | ✅ Naprawiony |
| 5 | Brak `UNIQUE` constraint na `date` w `PolishHoliday` | ✅ Naprawiony |
| 6 | Brak obsługi błędów parsowania daty | ✅ Naprawiony |
| 7 | Sprawdzanie duplikatów O(n²) w pętli | ✅ Naprawiony |

---

## NOWE BŁĘDY DO NAPRAWY

---

## BŁĄD A – KRYTYCZNY: Statusy obecności nigdy nie renderują właściwego koloru ani etykiety

### Przyczyna

Enum `AttendanceStatus` definiuje wartości jako angielskie stałe:
```java
// AttendanceStatus.java
public enum AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT
}
```

Tymczasem szablon `attendance_student.html` porównuje status z polskimi stringami, których enum nigdy nie zwróci:

```html
<!-- BŁĄD – porównanie z 'OBECNY' nigdy nie jest true, bo enum to 'PRESENT' -->
th:class="|badge ${a.status == 'OBECNY' ? 'badge-success' : (a.status == 'NIEOBECNY' ? 'badge-danger' : 'badge-warning')}|"
th:text="${a.status}"
```

**Skutki:**
- Każdy wpis obecności dostaje klasę `badge-warning` (żółty), niezależnie od faktycznego statusu.
- Wyświetlana etykieta to surowa angielska wartość enuma: `PRESENT`, `ABSENT`, `LATE`.

### Dotyczy pliku
`src/main/resources/templates/attendance_student.html` – linia ~85–88

### Naprawa

**Krok 1** – Dodać metodę `getLabel()` do enuma, aby centralnie zarządzać polskimi etykietami:

```java
// AttendanceStatus.java
public enum AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT;

    public String getLabel() {
        switch (this) {
            case PRESENT: return "Obecny";
            case LATE:    return "Spóźniony";
            case ABSENT:  return "Nieobecny";
            default:      return this.name();
        }
    }
}
```

**Krok 2** – Poprawić szablon tak, aby używał nazwy enuma (nie polskiego stringa) do porównania i `getLabel()` do wyświetlenia:

```html
<!-- attendance_student.html – zamienić błędny fragment na: -->
<span th:class="|badge ${a.status.name() == 'PRESENT' ? 'badge-success' :
                        (a.status.name() == 'ABSENT'  ? 'badge-danger'  : 'badge-warning')}|"
      th:text="${a.status.label}">
</span>
```

### Weryfikacja
Po zmianie: wpis z `PRESENT` → zielony badge „Obecny", `ABSENT` → czerwony „Nieobecny", `LATE` → żółty „Spóźniony".

---

## BŁĄD B – KRYTYCZNY: Usuwanie rekordów przez HTTP GET (podatność CSRF + naruszenie REST)

### Przyczyna

Operacje usuwania w trzech miejscach aplikacji są zrealizowane jako zwykłe linki HTML `<a href>`, co oznacza metodę HTTP **GET**. GET jest metodą idempotentną, przeznaczoną wyłącznie do odczytu. Operacje destrukcyjne muszą używać POST.

**Problem 1 – Podatność na CSRF via GET:**  
Osoba trzecia może osadzić link usuwający w emailu lub na stronie jako `<img src="http://localhost:8080/attendance/delete/5">`. Przeglądarka otwiera obraz, wykonuje GET, rekord zostaje usunięty bez wiedzy użytkownika. Spring Security chroni przed CSRF tylko dla metod POST/PUT/DELETE — nie dla GET.

**Problem 2 – Bypass Spring Security:**  
Spring Security domyślnie nie dodaje ochrony CSRF do żądań GET, więc `@GetMapping("/delete/...")` jest niezabezpieczony nawet jeśli CSRF jest włączony globalnie.

**Dotyczy trzech plików i ich kontrolerów:**

| Szablon | Kontroler | Endpoint |
|---------|-----------|----------|
| `attendance_student.html` | `AttendanceController.java` | `GET /attendance/delete/{id}` |
| `student_grades.html` | `GradeController.java` | `GET /grades/delete/{id}` |
| `teacher_dashboard.html` | `TeacherDashboardController.java` | `GET /teacher/exam/delete/{id}` |

### Naprawa

**Krok 1** – W każdym z trzech szablonów zamienić link `<a>` na mini-formularz POST:

```html
<!-- PRZED (błąd): -->
<a th:href="@{/attendance/delete/{id}(id=${a.id})}" class="text-danger">Usuń</a>

<!-- PO (poprawnie): -->
<form th:action="@{/attendance/delete/{id}(id=${a.id})}" method="post" style="display:inline;">
    <button type="submit" class="btn btn-danger"
            onclick="return confirm('Usunąć ten wpis obecności?')">Usuń</button>
</form>
```

Analogicznie dla `/grades/delete/{id}` i `/teacher/exam/delete/{id}`.

**Krok 2** – W każdym z trzech kontrolerów zmienić adnotację z `@GetMapping` na `@PostMapping`:

```java
// AttendanceController.java
// PRZED:
@GetMapping("/delete/{id}")
public String deleteAttendance(@PathVariable Long id) { ... }

// PO:
@PostMapping("/delete/{id}")
public String deleteAttendance(@PathVariable Long id) { ... }
```

Analogicznie w `GradeController.java` i `TeacherDashboardController.java`.

### Weryfikacja
Po zmianie: kliknięcie „Usuń" wysyła POST z tokenem CSRF → rekord usunięty. Wejście na URL `/attendance/delete/5` w przeglądarce → 405 Method Not Allowed (GET nie jest obsługiwany).

---

## BŁĄD C – WYSOKI: Strona edycji oceny (`edit_grade.html`) bez stylów i z błędnymi linkami

### Przyczyna

Plik `edit_grade.html` to niestylowany szkielet HTML bez dołączonego `style.css`, bez sidebara, bez layoutu aplikacji. Nawigacja wskazuje na nieistniejące URL-e (`/teachers`, `/students`, `/classes`) zamiast poprawnych (`/admin/teachers`, `/admin/students`, `/admin/classes`). Strona ta jest niespójna z całą resztą aplikacji i sprawia wrażenie niedokończonej.

### Dotyczy pliku
`src/main/resources/templates/edit_grade.html`

### Naprawa

Przepisać `edit_grade.html` zgodnie z layoutem reszty aplikacji — z `style.css`, sidebarem warunkowym (jak w `student_grades.html`) i poprawnymi linkami:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edytuj Ocenę – Dzienniczek</title>
    <link th:href="@{/css/style.css}" rel="stylesheet">
</head>
<body>
    <aside class="sidebar">
        <div class="sidebar-logo">
            <span sec:authorize="hasRole('ADMIN')">Dzienniczek Admin</span>
            <span sec:authorize="hasRole('TEACHER')">Dzienniczek Nauczyciela</span>
            <span sec:authorize="hasRole('SECRETARY')">Dzienniczek Sekretariat</span>
        </div>
        <nav>
            <ul class="nav-links">
                <th:block sec:authorize="hasRole('ADMIN')">
                    <li><a href="/admin">Dashboard</a></li>
                    <li><a href="/admin/students" class="active">Uczniowie</a></li>
                </th:block>
                <th:block sec:authorize="hasRole('TEACHER')">
                    <li><a href="/teacher/dashboard">Dashboard</a></li>
                    <li><a href="/teacher/classes" class="active">Moje Klasy</a></li>
                </th:block>
                <th:block sec:authorize="hasRole('SECRETARY')">
                    <li><a href="/secretary/dashboard">Dashboard</a></li>
                    <li><a href="/admin/students" class="active">Uczniowie</a></li>
                </th:block>
            </ul>
        </nav>
        <form th:action="@{/logout}" method="post" style="margin-top: auto;">
            <button type="submit" class="btn btn-danger" style="width: 100%;">Wyloguj</button>
        </form>
    </aside>

    <main class="main-content">
        <div class="header" style="margin-bottom: 2rem;">
            <h1 th:text="|Edytuj ocenę z: ${grade.subject.name}|">Edytuj Ocenę</h1>
            <a th:href="@{/grades/student/{sId}/subject/{subId}(sId=${grade.student.id},subId=${grade.subject.id})}"
               class="btn btn-ghost" style="margin-top: 0.75rem;">← Powrót do ocen</a>
        </div>

        <div class="card" style="max-width: 480px; padding: 2rem;">
            <form th:action="@{/grades/edit/{id}(id=${grade.id})}" method="post" th:object="${grade}">
                <div class="form-group">
                    <label>Wartość oceny</label>
                    <select th:field="*{value}" class="input-control" required>
                        <option th:each="v : ${ {'1','1+','2-','2','2+','3-','3','3+','4-','4','4+','5-','5','5+','6-','6'} }"
                                th:value="${v}" th:text="${v}"></option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Data wystawienia</label>
                    <input type="date" th:field="*{date}" class="input-control" required/>
                </div>
                <div style="display: flex; gap: 1rem; margin-top: 1.5rem;">
                    <button type="submit" class="btn btn-primary">Zapisz zmiany</button>
                    <a th:href="@{/grades/student/{sId}/subject/{subId}(sId=${grade.student.id},subId=${grade.subject.id})}"
                       class="btn btn-ghost">Anuluj</a>
                </div>
            </form>
        </div>
    </main>
</body>
</html>
```

### Weryfikacja
Po zmianie: strona edycji oceny wygląda spójnie z resztą aplikacji, sidebar jest widoczny, linki nawigacyjne działają poprawnie.

---

## BŁĄD D – WYSOKI: NullPointerException gdy przedmiot nie ma przypisanego nauczyciela

### Przyczyna

W `GradeController.java` metoda `viewSubjectsForStudent()` bezwarunkowo wywołuje `sub.getTeacher().getFirstName()`:

```java
// BŁĄD – rzuci NullPointerException jeśli teacher == null
String initials = sub.getTeacher().getFirstName().charAt(0) + "."
                + sub.getTeacher().getLastName().charAt(0) + ".";
```

Pole `teacher` w encji `Subject` jest opcjonalne (brak `nullable = false`, brak `@NotNull`). Administrator lub sekretariat może dodać przedmiot bez przypisanego nauczyciela. Wejście ucznia lub nauczyciela na stronę ocen takiego przedmiotu zakończy się błędem HTTP 500 z wyjątkiem `NullPointerException`.

### Dotyczy pliku
`src/main/java/com/example/demo/controller/GradeController.java` – linia ~74

### Naprawa

Dodać sprawdzenie `null` przed wywołaniem metod na `teacher`:

```java
// GradeController.java – metoda viewSubjectsForStudent()
// PRZED:
String initials = sub.getTeacher().getFirstName().charAt(0) + "."
                + sub.getTeacher().getLastName().charAt(0) + ".";

// PO:
String initials = (sub.getTeacher() != null)
    ? sub.getTeacher().getFirstName().charAt(0) + "."
      + sub.getTeacher().getLastName().charAt(0) + "."
    : "–";
```

Dodatkowo warto zabezpieczyć podobne wywołanie w szablonie `attendance_student.html`, gdzie dla każdej opcji w select wyświetlany jest nauczyciel:
```html
<!-- attendance_student.html -->
<!-- PRZED: -->
th:text="|${sub.name} (${sub.teacher.lastName})|"

<!-- PO: -->
th:text="|${sub.name}${sub.teacher != null ? ' (' + sub.teacher.lastName + ')' : ''}|"
```

### Weryfikacja
Po zmianie: strona ocen dla przedmiotu bez nauczyciela ładuje się poprawnie, inicjały wyświetlają się jako „–".

---

## BŁĄD E – ŚREDNI: Endpointy REST API dostępne dla każdej roli (brak autoryzacji)

### Przyczyna

W `SecurityConfig.java` brak jawnej reguły dla `/api/**`. Endpointy REST trafiają w ostatnią regułę `.anyRequest().authenticated()`, co oznacza że **każdy zalogowany użytkownik** — w tym uczeń — może przez REST API odczytać lub zmodyfikować dane dowolnego ucznia.

Kontrolery webowe (`AttendanceController`, `GradeController`) mają własne security checki blokujące uczniów. REST API (`AttendanceRestController`, `GradeRestController`, `StudentRestController`) **nie ma żadnych takich zabezpieczeń**.

Przykład ataku: zalogowany uczeń wysyła `GET /api/attendance/student/99` i widzi obecności innego ucznia.

### Dotyczy pliku
`src/main/java/com/example/demo/config/SecurityConfig.java`

### Naprawa

Dodać jawną regułę dla `/api/**` **przed** regułą `anyRequest()` w `SecurityConfig.filterChain()`:

```java
// SecurityConfig.java – metoda filterChain()
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
    .requestMatchers("/admin/**").hasAnyRole("ADMIN", "DIRECTOR", "SECRETARY")
    .requestMatchers("/director/**").hasRole("DIRECTOR")
    .requestMatchers("/secretary/**").hasRole("SECRETARY")
    .requestMatchers("/teacher/**").hasRole("TEACHER")
    .requestMatchers("/student/**").hasRole("STUDENT")
    .requestMatchers("/messages/**").authenticated()
    .requestMatchers("/grades/**", "/attendance/**").hasAnyRole("ADMIN", "TEACHER", "SECRETARY", "STUDENT")
    .requestMatchers("/schedules/**").authenticated()
    .requestMatchers("/schedule-admin/**").hasAnyRole("ADMIN", "DIRECTOR", "SECRETARY")
    // NOWA REGUŁA – REST API tylko dla uprawnionych ról:
    .requestMatchers("/api/**").hasAnyRole("ADMIN", "DIRECTOR", "SECRETARY", "TEACHER")
    .anyRequest().authenticated())
```

### Weryfikacja
Po zmianie: zalogowany uczeń wywołujący `GET /api/attendance/student/1` dostaje HTTP 403 Forbidden. Nauczyciel i admin mają dostęp normalnie.

---

## Podsumowanie priorytetów

| # | Błąd | Plik(i) | Priorytet |
|---|------|---------|-----------|
| A | Statusy obecności – złe porównania z enumem, brak polskich etykiet, błędne kolory | `attendance_student.html`, `AttendanceStatus.java` | 🔴 KRYTYCZNY |
| B | Usuwanie rekordów przez GET – podatność CSRF, naruszenie REST | `attendance_student.html`, `student_grades.html`, `teacher_dashboard.html` + 3 kontrolery | 🔴 KRYTYCZNY |
| C | `edit_grade.html` bez stylów, bez layoutu, z błędnymi linkami | `edit_grade.html` | 🟡 WYSOKI |
| D | NPE gdy przedmiot nie ma nauczyciela w `GradeController` | `GradeController.java`, `attendance_student.html` | 🟡 WYSOKI |
| E | REST API bez autoryzacji – uczniowie mogą czytać dane innych | `SecurityConfig.java` | 🟠 ŚREDNI |

---

## Kolejność implementacji (zalecana)

1. **Błąd B** – zmiana GET → POST dla usuwania we wszystkich 3 szablonach i 3 kontrolerach (jedno przejście po całym projekcie)
2. **Błąd A** – dodanie `getLabel()` do enuma + poprawka porównań w szablonie (dwa pliki)
3. **Błąd D** – null-check w `GradeController` + zabezpieczenie szablonu `attendance_student.html` (przy okazji Błędu A, ten sam plik)
4. **Błąd E** – jedna linijka w `SecurityConfig.java`
5. **Błąd C** – przepisanie `edit_grade.html` (największa zmiana wizualna, ale niekrytyczna funkcjonalnie)
