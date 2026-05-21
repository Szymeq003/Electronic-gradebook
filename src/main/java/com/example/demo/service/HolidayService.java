package com.example.demo.service;

import com.example.demo.model.PolishHoliday;
import com.example.demo.repository.PolishHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HolidayService {

    private final PolishHolidayRepository polishHolidayRepository;

    /** Zwraca nazwę święta lub empty jeśli dzień normalny. */
    public Optional<String> getHolidayName(LocalDate date) {
        Optional<String> legal = getLegalHolidayName(date);
        if (legal.isPresent()) return legal;
        return polishHolidayRepository.findByDate(date)
                .filter(PolishHoliday::isSchoolFree)
                .map(PolishHoliday::getName);
    }

    public boolean isHoliday(LocalDate date) {
        return getHolidayName(date).isPresent();
    }

    /** Mapa {data -> nazwa} dla zakresu dat – używana przez kontroler. */
    public Map<LocalDate, String> getHolidaysInRange(LocalDate start, LocalDate end) {
        Map<LocalDate, String> result = new LinkedHashMap<>();
        LocalDate d = start;
        while (!d.isAfter(end)) {
            final LocalDate current = d;
            getHolidayName(current).ifPresent(name -> result.put(current, name));
            d = d.plusDays(1);
        }
        return result;
    }

    /** Polskie święta ustawowe – stałe + Wielkanoc */
    private Optional<String> getLegalHolidayName(LocalDate date) {
        int year = date.getYear();
        Map<LocalDate, String> h = new HashMap<>();
        h.put(LocalDate.of(year, Month.JANUARY,   1), "Nowy Rok");
        h.put(LocalDate.of(year, Month.JANUARY,   6), "Trzech Króli");
        h.put(LocalDate.of(year, Month.MAY,        1), "Święto Pracy");
        h.put(LocalDate.of(year, Month.MAY,        3), "Konstytucja 3 Maja");
        h.put(LocalDate.of(year, Month.AUGUST,    15), "Wniebowzięcie NMP");
        h.put(LocalDate.of(year, Month.NOVEMBER,   1), "Wszystkich Świętych");
        h.put(LocalDate.of(year, Month.NOVEMBER,  11), "Święto Niepodległości");
        h.put(LocalDate.of(year, Month.DECEMBER,  25), "Boże Narodzenie");
        h.put(LocalDate.of(year, Month.DECEMBER,  26), "Boże Narodzenie (2. dzień)");
        LocalDate easter = computeEaster(year);
        h.put(easter,               "Wielkanoc");
        h.put(easter.plusDays(1),   "Poniedziałek Wielkanocny");
        h.put(easter.plusDays(49),  "Zielone Świątki");
        h.put(easter.plusDays(60),  "Boże Ciało");
        return Optional.ofNullable(h.get(date));
    }

    /** Algorytm Meeusa/Jonesa/Butchera obliczający datę Wielkanocy */
    private LocalDate computeEaster(int year) {
        int a = year % 19, b = year / 100, c = year % 100;
        int d = b / 4, e = b % 4, f = (b + 8) / 25;
        int g = (b - f + 1) / 3, hh = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - hh - k) % 7;
        int m = (a + 11 * hh + 22 * l) / 451;
        int month = (hh + l - 7 * m + 114) / 31;
        int day   = ((hh + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    /** CRUD dla szkolnych dni wolnych wpisanych przez admina */
    public List<PolishHoliday> getAllCustomHolidays() {
        return polishHolidayRepository.findAll();
    }

    public boolean existsCustomHolidayByDate(LocalDate date) {
        return polishHolidayRepository.existsByDate(date);
    }

    public PolishHoliday save(PolishHoliday h) {
        return polishHolidayRepository.save(h);
    }

    public void delete(Long id) {
        polishHolidayRepository.deleteById(id);
    }
}
