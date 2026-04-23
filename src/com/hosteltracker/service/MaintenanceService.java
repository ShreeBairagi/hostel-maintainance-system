package com.hosteltracker.service;

import com.hosteltracker.exception.DuplicateRequestException;
import com.hosteltracker.exception.InvalidRoomException;
import com.hosteltracker.model.enums.RequestStatus;
import com.hosteltracker.model.request.CarpentryRequest;
import com.hosteltracker.model.request.ElectricalRequest;
import com.hosteltracker.model.request.PlumbingRequest;
import com.hosteltracker.model.request.Request;
import com.hosteltracker.model.user.MaintenanceStaff;
import com.hosteltracker.util.RequestIDGenerator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MaintenanceService {
    private final List<Request> requests = new ArrayList<>();
    private final List<MaintenanceStaff> maintenanceStaffMembers = new ArrayList<>();
    private final Set<String> openRooms = new HashSet<>();
    private final Map<Integer, Request> requestLookup = new HashMap<>();
    private final File dataDirectory = new File("data");
    private final File requestLogFile = new File(dataDirectory, "requests.csv");
    private final File ratingLogFile = new File(dataDirectory, "ratings.csv");

    public MaintenanceService() {
        initializeFiles();
        seedDefaultStaff();
        synchronizeRequestIdGenerator();
    }

    public Request createRequest(String roomNumber, String category, String description, String photoPath)
            throws InvalidRoomException, DuplicateRequestException {
        validateRoom(roomNumber);

        if (openRooms.contains(roomNumber)) {
            throw new DuplicateRequestException("Room " + roomNumber + " already has an open request.");
        }

        Request request;
        if ("plumbing".equalsIgnoreCase(category)) {
            request = new PlumbingRequest(roomNumber, description, photoPath);
        } else if ("electrical".equalsIgnoreCase(category)) {
            request = new ElectricalRequest(roomNumber, description, photoPath);
        } else if ("carpentry".equalsIgnoreCase(category)) {
            request = new CarpentryRequest(roomNumber, description, photoPath);
        } else {
            throw new IllegalArgumentException("Unknown category. Use plumbing, electrical, or carpentry.");
        }

        requests.add(request);
        openRooms.add(roomNumber);
        requestLookup.put(request.getRequestId(), request);
        appendRequestToCsv(request);
        return request;
    }

    public void assignRequest(int requestId, String staffName) {
        Request request = getRequestByIdOrThrow(requestId);
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new IllegalStateException("Cannot assign a completed request.");
        }
        if (staffName == null || staffName.trim().isEmpty()) {
            throw new IllegalArgumentException("Please select a maintenance worker.");
        }
        MaintenanceStaff staff = findStaffByName(staffName);
        if (staff == null) {
            throw new IllegalArgumentException("Selected maintenance worker is not registered.");
        }
        if (!isStaffAvailableForRequest(staff, request)) {
            throw new IllegalStateException("Selected worker already has an active task.");
        }
        request.assignTo(staffName);
        appendRequestToCsv(request);
    }

    public void addMaintenanceStaff(String userId, String name, String specialty) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee ID is required.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee name is required.");
        }
        if (specialty == null || specialty.trim().isEmpty()) {
            throw new IllegalArgumentException("Specialty is required.");
        }
        for (MaintenanceStaff staff : maintenanceStaffMembers) {
            if (staff.getUserId().equalsIgnoreCase(userId.trim())) {
                throw new IllegalArgumentException("Employee ID already exists.");
            }
        }
        maintenanceStaffMembers.add(new MaintenanceStaff(userId.trim(), name.trim(), specialty.trim()));
    }

    public List<MaintenanceStaff> getAllMaintenanceStaff() {
        return new ArrayList<>(maintenanceStaffMembers);
    }

    public List<MaintenanceStaff> getAvailableStaffForRequest(int requestId) {
        Request request = getRequestByIdOrThrow(requestId);
        List<MaintenanceStaff> available = new ArrayList<>();
        for (MaintenanceStaff staff : maintenanceStaffMembers) {
            if (isStaffSuitableForCategory(staff, request.getCategory())
                    && isStaffAvailableForRequest(staff, request)) {
                available.add(staff);
            }
        }
        return available;
    }

    public void updateRequestStatus(int requestId, RequestStatus newStatus) {
        Request request = getRequestByIdOrThrow(requestId);
        request.updateStatus(newStatus);
        if (newStatus == RequestStatus.COMPLETED) {
            openRooms.remove(request.getRoomNumber());
        }
        appendRequestToCsv(request);
    }

    public void rateCompletedRequest(int requestId, int rating) {
        Request request = getRequestByIdOrThrow(requestId);
        request.rateService(rating);
        appendRequestToCsv(request);
        appendRatingToCsv(request);
    }

    public Request getRequestById(int requestId) {
        return requestLookup.get(requestId);
    }

    public List<Request> getAllRequests() {
        return new ArrayList<>(requests);
    }

    public long getResponseTimeHours(int requestId) {
        Request request = getRequestByIdOrThrow(requestId);
        LocalDateTime endTime = request.getCompletedAt();
        if (endTime == null) {
            throw new IllegalStateException("Request is not completed yet.");
        }
        return Duration.between(request.getCreatedAt(), endTime).toHours();
    }

    public Map<String, Integer> getIssueDistribution() {
        Map<String, Integer> counts = new HashMap<>();
        for (Request request : requests) {
            counts.merge(request.getCategory(), 1, Integer::sum);
        }
        return counts;
    }

    private Request getRequestByIdOrThrow(int requestId) {
        Request request = requestLookup.get(requestId);
        if (request == null) {
            throw new IllegalArgumentException("No request found with ID " + requestId);
        }
        return request;
    }

    private void validateRoom(String roomNumber) throws InvalidRoomException {
        if (roomNumber == null || !roomNumber.matches("[A-Za-z]-\\d{3}")) {
            throw new InvalidRoomException("Room must be in format Block-Number, e.g., A-101.");
        }
    }

    private void initializeFiles() {
        try {
            if (!dataDirectory.exists() && !dataDirectory.mkdirs()) {
                throw new IOException("Failed to create data directory.");
            }
            if (!requestLogFile.exists()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(requestLogFile, true))) {
                    writer.write("RequestID,Room,Category,Description,PhotoPath,Status,AssignedTo,Priority,CreatedAt,CompletedAt,Rating");
                    writer.newLine();
                }
            }
            if (!ratingLogFile.exists()) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(ratingLogFile, true))) {
                    writer.write("RequestID,Category,Rating");
                    writer.newLine();
                }
            }
            removeBlankLines(requestLogFile);
            removeBlankLines(ratingLogFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize CSV files.", e);
        }
    }

    private void appendRequestToCsv(Request request) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(requestLogFile, true))) {
            writer.write(request.toCsvRow());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write request log.", e);
        }
    }

    private void appendRatingToCsv(Request request) {
        if (request.getRating() == null) {
            return;
        }
        String newEntry = request.getRequestId() + "," + request.getCategory() + "," + request.getRating();
        try {
            List<String> lines = ratingLogFile.exists()
                    ? Files.readAllLines(ratingLogFile.toPath())
                    : new ArrayList<>();
            if (lines.isEmpty()) {
                lines.add("RequestID,Category,Rating");
            }
            List<String> filtered = new ArrayList<>();
            filtered.add(lines.get(0));
            String requestPrefix = request.getRequestId() + ",";
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (!line.startsWith(requestPrefix)) {
                    filtered.add(line);
                }
            }
            filtered.add(newEntry);
            Files.write(ratingLogFile.toPath(), filtered);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write rating log.", e);
        }
    }

    private void seedDefaultStaff() {
        maintenanceStaffMembers.add(new MaintenanceStaff("EMP-101", "Ravi Kumar", "plumbing"));
        maintenanceStaffMembers.add(new MaintenanceStaff("EMP-102", "Anita Das", "electrical"));
        maintenanceStaffMembers.add(new MaintenanceStaff("EMP-103", "Suresh Patel", "carpentry"));
    }

    private MaintenanceStaff findStaffByName(String name) {
        for (MaintenanceStaff staff : maintenanceStaffMembers) {
            if (staff.getName().equalsIgnoreCase(name.trim())) {
                return staff;
            }
        }
        return null;
    }

    private boolean isStaffSuitableForCategory(MaintenanceStaff staff, String category) {
        return staff.getSpecialty().equalsIgnoreCase(category)
                || "all".equalsIgnoreCase(staff.getSpecialty())
                || "general".equalsIgnoreCase(staff.getSpecialty());
    }

    private boolean isStaffAvailableForRequest(MaintenanceStaff staff, Request targetRequest) {
        for (Request request : requests) {
            if (request.getRequestId() == targetRequest.getRequestId()) {
                continue;
            }
            if (request.getStatus() != RequestStatus.COMPLETED
                    && request.getAssignedTo() != null
                    && request.getAssignedTo().equalsIgnoreCase(staff.getName())) {
                return false;
            }
        }
        return true;
    }

    private void removeBlankLines(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        List<String> lines = Files.readAllLines(file.toPath());
        if (lines.isEmpty()) {
            return;
        }
        List<String> cleaned = new ArrayList<>();
        cleaned.add(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                cleaned.add(line);
            }
        }
        Files.write(file.toPath(), cleaned);
    }

    private void synchronizeRequestIdGenerator() {
        if (!requestLogFile.exists()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(requestLogFile.toPath());
            int maxId = 1000;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length == 0) {
                    continue;
                }
                try {
                    int id = Integer.parseInt(parts[0].trim());
                    if (id > maxId) {
                        maxId = id;
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore malformed lines and keep service running.
                }
            }
            RequestIDGenerator.getInstance().ensureAtLeast(maxId);
        } catch (IOException e) {
            throw new RuntimeException("Failed to sync request ID generator from CSV.", e);
        }
    }
}

