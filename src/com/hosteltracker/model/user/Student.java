package com.hosteltracker.model.user;

public class Student extends User {
    private final String roomNumber;

    public Student(String userId, String name, String roomNumber) {
        super(userId, name);
        this.roomNumber = roomNumber;
    }

    public String getRoomNumber() {
        return roomNumber;
    }
}

