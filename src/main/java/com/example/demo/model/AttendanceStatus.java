package com.example.demo.model;

public enum AttendanceStatus {
    PRESENT,
    LATE,
    ABSENT,
    EXCUSED;

    public String getLabel() {
        switch (this) {
            case PRESENT: return "Obecny";
            case LATE:    return "Spóźniony";
            case ABSENT:  return "Nieobecny";
            case EXCUSED: return "Usprawiedliwiony";
            default:      return this.name();
        }
    }
}
