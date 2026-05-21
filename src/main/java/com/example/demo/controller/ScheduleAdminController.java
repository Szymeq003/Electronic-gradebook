package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.HolidayService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/schedule-admin")
@RequiredArgsConstructor
public class ScheduleAdminController {

    private final ScheduleRepository scheduleRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final RoomRepository roomRepository;
    private final HolidayService holidayService;

    /** Lista klas do wyboru */
    @GetMapping
    public String index(Model model, Authentication authentication) {
        model.addAttribute("classes", schoolClassRepository.findAll());
        String backUrl = "/secretary/dashboard"; // domyślny
        if (authentication != null) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                backUrl = "/admin/dashboard";
            } else if (authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DIRECTOR"))) {
                backUrl = "/director/dashboard";
            }
        }
        model.addAttribute("backUrl", backUrl);
        return "schedule_admin_index";
    }

    /** Plan wybranej klasy z formularzem dodawania */
    @GetMapping("/class/{classId}")
    public String classSchedule(@PathVariable Long classId, Model model) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Klasa nie znaleziona"));

        List<Schedule> schedules = scheduleRepository.findBySchoolClassId(classId);
        
        // Sortowanie chronologiczne według kolejności dni tygodnia (Pon=1 … Nd=7) i start time
        schedules.sort(Comparator
                .comparingInt((Schedule s) -> s.getDayOfWeek().getValue())
                .thenComparing(Schedule::getStartTime));

        model.addAttribute("schoolClass", schoolClass);
        model.addAttribute("schedules", schedules);
        model.addAttribute("subjects", subjectRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        model.addAttribute("days", DayOfWeek.values());
        return "schedule_admin_edit";
    }

    /** Dodaj lekcję */
    @PostMapping("/class/{classId}/add")
    public String addLesson(@PathVariable Long classId,
                            @RequestParam Long subjectId,
                            @RequestParam Long roomId,
                            @RequestParam String dayOfWeek,
                            @RequestParam String startTime,
                            @RequestParam String endTime) {
        Schedule s = new Schedule();
        s.setSchoolClass(schoolClassRepository.findById(classId).orElseThrow());
        s.setSubject(subjectRepository.findById(subjectId).orElseThrow());
        s.setRoom(roomRepository.findById(roomId).orElseThrow());
        s.setDayOfWeek(DayOfWeek.valueOf(dayOfWeek));
        s.setStartTime(LocalTime.parse(startTime));
        s.setEndTime(LocalTime.parse(endTime));
        scheduleRepository.save(s);
        return "redirect:/schedule-admin/class/" + classId;
    }

    /** Usuń lekcję */
    @PostMapping("/delete/{scheduleId}")
    public String deleteLesson(@PathVariable Long scheduleId,
                               @RequestParam Long classId) {
        scheduleRepository.deleteById(scheduleId);
        return "redirect:/schedule-admin/class/" + classId;
    }

    /** Panel zarządzania dniami wolnymi */
    @GetMapping("/holidays")
    public String holidays(Model model) {
        model.addAttribute("holidays", holidayService.getAllCustomHolidays());
        return "schedule_admin_holidays";
    }

    @PostMapping("/holidays/add")
    public String addHoliday(@RequestParam String name,
                             @RequestParam String dateFrom,
                             @RequestParam(required = false) String dateTo,
                             RedirectAttributes redirectAttributes) {
        try {
            LocalDate from = LocalDate.parse(dateFrom);
            LocalDate to = (dateTo != null && !dateTo.isBlank()) ? LocalDate.parse(dateTo) : from;

            if (to.isBefore(from)) {
                redirectAttributes.addFlashAttribute("error", "Data 'Do' nie może być wcześniejsza niż data 'Od'.");
                return "redirect:/schedule-admin/holidays";
            }

            // Wydajne zapisywanie każdego dnia zakresu z weryfikacją O(1)
            LocalDate current = from;
            while (!current.isAfter(to)) {
                final LocalDate day = current;
                boolean exists = holidayService.existsCustomHolidayByDate(day);
                if (!exists) {
                    PolishHoliday h = new PolishHoliday();
                    h.setName(name);
                    h.setDate(day);
                    h.setSchoolFree(true);
                    holidayService.save(h);
                }
                current = current.plusDays(1);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Nieprawidłowy format daty. Użyj formatu RRRR-MM-DD.");
        }
        return "redirect:/schedule-admin/holidays";
    }

    @PostMapping("/holidays/delete/{id}")
    public String deleteHoliday(@PathVariable Long id) {
        holidayService.delete(id);
        return "redirect:/schedule-admin/holidays";
    }
}
