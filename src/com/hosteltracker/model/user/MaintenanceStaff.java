package com.hosteltracker.model.user;

public class MaintenanceStaff extends User {
    private final String specialty;

    public MaintenanceStaff(String userId, String name, String specialty) {
        super(userId, name);
        this.specialty = specialty;
    }

    public String getSpecialty() {
        return specialty;
    }
}

