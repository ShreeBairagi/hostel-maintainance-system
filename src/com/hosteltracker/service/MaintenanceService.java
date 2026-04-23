package com.hosteltracker.service;

import com.hosteltracker.exception.DuplicateRequestException;
import com.hosteltracker.exception.InvalidRoomException;
import com.hosteltracker.model.enums.RequestStatus;
import com.hosteltracker.model.request.CarpentryRequest;
import com.hosteltracker.model.request.ElectricalRequest;
import com.hosteltracker.model.request.PlumbingRequest;
import com.hosteltracker.model.request.Request;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private final Set<String> openRooms = new HashSet<>();
    private final Map<Integer, Request> requestLookup = new HashMap<>();
    private final File dataDirectory = new File("data");
    private final File requestLogFile = new File(dataDirectory, "requests.csv");
    private final File ratingLogFile = new File(dataDirectory, "ratings.csv");

    public MaintenanceService() {
        initializeFiles();
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
        request.assignTo(staffName);
        appendRequestToCsv(request);
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
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ratingLogFile, true))) {
            writer.write(request.getRequestId() + "," + request.getCategory() + "," + request.getRating());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write rating log.", e);
        }
    }
}

