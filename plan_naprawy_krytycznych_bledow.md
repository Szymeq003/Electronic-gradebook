# Plan naprawy krytycznych błędów — System Wiadomości
**Projekt:** Dzienniczek-szkolny  
**Data:** 2026-05-13  
**Dotyczy:** `MessageController`, `MessageService`, walidacja formularza

---

## Błąd #1 — Nieistniejący szablon Thymeleaf w `getMessage`

**Plik:** `controller/MessageController.java`  
**Linia:** ~67  
**Opis:** Kontroler zwraca fragment `"message_view :: messageContent"`, ale plik `message_view.html` nie istnieje w projekcie. Każde wywołanie AJAX przez `loadMessage()` kończy się błędem 500 i użytkownik nigdy nie zobaczy treści wiadomości.

### Kroki naprawy

1. Otworzyć `MessageController.java` i znaleźć metodę `getMessage`.
2. Zmienić wartość zwracaną z:
   ```java
   return "message_view :: messageContent";
   ```
   na:
   ```java
   return "messages :: messageContent";
   ```
3. Zweryfikować, że fragment `th:fragment="messageContent"` istnieje w `messages.html` (jest — linia ~55).
4. Uruchomić aplikację i kliknąć wiadomość na liście — treść powinna załadować się po prawej stronie bez błędu w konsoli przeglądarki.

**Oczekiwany rezultat:** Kliknięcie wiadomości ładuje jej treść przez AJAX bez błędu 500.

---

## Błąd #2 — Brak walidacji pól formularza wysyłania wiadomości

**Pliki:** `controller/MessageController.java`, `service/MessageService.java`  
**Opis:** Endpoint `POST /messages/send` przyjmuje `subject` i `content` jako surowe `@RequestParam` bez żadnej walidacji. Atrybut `required` w HTML można ominąć bezpośrednim żądaniem HTTP. Możliwe jest wysłanie pustej wiadomości lub wiadomości o nieograniczonej długości, co może prowadzić do błędów bazy danych lub nadużyć.

### Kroki naprawy

1. Stworzyć klasę DTO `SendMessageRequest.java` w pakiecie `model` (lub nowym `dto`):
   ```java
   package com.example.demo.model;

   import jakarta.validation.constraints.NotBlank;
   import jakarta.validation.constraints.Size;

   public class SendMessageRequest {

       private Long recipientId;

       @NotBlank(message = "Temat nie może być pusty")
       @Size(max = 200, message = "Temat może mieć maksymalnie 200 znaków")
       private String subject;

       @NotBlank(message = "Treść wiadomości nie może być pusta")
       @Size(max = 5000, message = "Treść może mieć maksymalnie 5000 znaków")
       private String content;

       // gettery i settery lub @Data z Lombok
   }
   ```

2. W `MessageController.java` zmienić sygnaturę metody `sendMessage`:
   ```java
   // PRZED
   @PostMapping("/send")
   public String sendMessage(@RequestParam Long recipientId,
                             @RequestParam String subject,
                             @RequestParam String content) {

   // PO
   @PostMapping("/send")
   public String sendMessage(@Valid @ModelAttribute SendMessageRequest request,
                             BindingResult bindingResult,
                             Model model) {
       if (bindingResult.hasErrors()) {
           AppUser currentUser = securityService.getCurrentAppUser()
               .orElseThrow(() -> new IllegalStateException("Użytkownik nie jest zalogowany"));
           model.addAttribute("recipients", messageService.getAvailableRecipients(currentUser));
           model.addAttribute("errors", bindingResult.getAllErrors());
           return "new_message";
       }
       // ... reszta logiki
   }
   ```

3. Dodać import `jakarta.validation.Valid` i `org.springframework.validation.BindingResult` do kontrolera.

4. W szablonie `new_message.html` dodać wyświetlanie błędów walidacji:
   ```html
   <div th:if="${errors}" class="alert alert-danger">
       <p th:each="error : ${errors}" th:text="${error.defaultMessage}"></p>
   </div>
   ```

5. Sprawdzić, że `spring-boot-starter-validation` jest w `build.gradle`:
   ```groovy
   implementation 'org.springframework.boot:spring-boot-starter-validation'
   ```

**Oczekiwany rezultat:** Wysłanie pustego formularza lub zbyt długiej wiadomości przez HTTP skutkuje czytelnym komunikatem błędu, a nie wyjątkiem serwera.

---

## Błąd #3 — Brak `@Transactional` w metodzie `sendMessage`

**Plik:** `service/MessageService.java`  
**Opis:** Metoda `sendMessage` wykonuje dwie operacje na bazie danych (odczyt `AppUser` + zapis `Message`) bez transakcji. Brak `@Transactional` oznacza, że przy błędzie między tymi operacjami dane mogą być niespójne. Dodatkowo lazy-loading powiązań encji (np. `sender.getTeacher()`) poza sesją Hibernate może rzucać `LazyInitializationException`.

### Kroki naprawy

1. Dodać adnotację `@Transactional` do metody `sendMessage` w `MessageService.java`:
   ```java
   import org.springframework.transaction.annotation.Transactional;

   @Transactional
   public void sendMessage(AppUser sender, Long recipientId, String subject, String content) {
       AppUser recipient = appUserRepository.findById(recipientId)
               .orElseThrow(() -> new IllegalArgumentException("Odbiorca nie istnieje"));

       if (!canSendTo(sender, recipient)) {
           throw new IllegalStateException("Nie masz uprawnień do wysłania wiadomości do tego użytkownika");
       }

       Message message = Message.builder()
               .sender(sender)
               .recipient(recipient)
               .subject(subject)
               .content(content)
               .sentAt(LocalDateTime.now())
               .isRead(false)
               .build();

       messageRepository.save(message);
   }
   ```

2. Dla spójności dodać `@Transactional` również do `getMessage`, ponieważ modyfikuje ona flagę `isRead`:
   ```java
   @Transactional
   public Message getMessage(Long id, AppUser reader) { ... }
   ```

3. Metody tylko odczytujące (`getInbox`, `getSentMessages`, `getAvailableRecipients`) oznaczyć jako `@Transactional(readOnly = true)` — poprawia wydajność:
   ```java
   @Transactional(readOnly = true)
   public List<Message> getInbox(AppUser user) { ... }
   ```

**Oczekiwany rezultat:** Operacje na bazie są atomowe, lazy-loading działa poprawnie w ramach sesji, a baza pozostaje spójna nawet przy nieoczekiwanych błędach.

---

## Kolejność wdrożenia

| Krok | Błąd | Szacowany czas |
|------|------|----------------|
| 1 | Naprawa nazwy szablonu (#1) | ~5 min |
| 2 | Dodanie `@Transactional` (#3) | ~10 min |
| 3 | Stworzenie DTO + walidacja (#2) | ~30 min |

> Zaleca się wdrożenie w tej kolejności — błąd #1 można przetestować natychmiast, a #3 wymaga też zmian w szablonie HTML.

---

## Weryfikacja po naprawach

- [ ] Kliknięcie wiadomości w skrzynce ładuje jej treść (brak błędu 500 w DevTools)
- [ ] Wysłanie formularza z pustym tematem wraca do formularza z komunikatem błędu
- [ ] Wysłanie wiadomości z treścią >5000 znaków jest zablokowane
- [ ] Bezpośredni `curl -X POST /messages/send` z pustymi polami zwraca ponowny formularz, nie wyjątek
- [ ] Logi aplikacji nie pokazują `LazyInitializationException` podczas operacji na wiadomościach
