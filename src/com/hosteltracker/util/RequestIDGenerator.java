package com.hosteltracker.util;

public class RequestIDGenerator {
    private static RequestIDGenerator instance;
    private int currentId;

    private RequestIDGenerator() {
        currentId = 1000;
    }

    public static synchronized RequestIDGenerator getInstance() {
        if (instance == null) {
            instance = new RequestIDGenerator();
        }
        return instance;
    }

    public synchronized int nextId() {
        return ++currentId;
    }

    public synchronized void ensureAtLeast(int existingMaxId) {
        if (existingMaxId > currentId) {
            currentId = existingMaxId;
        }
    }
}

