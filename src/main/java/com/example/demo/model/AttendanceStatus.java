package com.example.demo.model;

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
