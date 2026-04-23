package com.hosteltracker.model.request;

import com.hosteltracker.model.enums.Priority;
import com.hosteltracker.model.enums.RequestStatus;
import com.hosteltracker.model.interfaces.Ratable;
import com.hosteltracker.model.interfaces.Trackable;
import com.hosteltracker.util.RequestIDGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class Request implements Trackable, Ratable {
    private final int requestId;
    private final String roomNumber;
    private final String issueDescription;
    private final String photoPath;
    private final LocalDateTime createdAt;
    private RequestStatus status;
    private String assignedTo;
    private Integer rating;
    private LocalDateTime completedAt;

    protected Request(String roomNumber, String issueDescription, String photoPath) {
        this.requestId = RequestIDGenerator.getInstance().nextId();
        this.roomNumber = roomNumber;
        this.issueDescription = issueDescription;
        this.photoPath = photoPath;
        this.createdAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
    }

    public int getRequestId() {
        return requestId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void assignTo(String staffName) {
        assignedTo = staffName;
        updateStatus(RequestStatus.ASSIGNED);
    }

    @Override
    public RequestStatus getStatus() {
        return status;
    }

    @Override
    public void updateStatus(RequestStatus newStatus) {
        status = newStatus;
        if (newStatus == RequestStatus.COMPLETED) {
            completedAt = LocalDateTime.now();
        }
    }

    @Override
    public void rateService(int rating) {
        if (status != RequestStatus.COMPLETED) {
            throw new IllegalStateException("Request is not completed yet.");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }
        this.rating = rating;
    }

    @Override
    public Integer getRating() {
        return rating;
    }

    public abstract Priority getPriority();

    public abstract String getCategory();

    public String toCsvRow() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return requestId + "," +
                roomNumber + "," +
                getCategory() + "," +
                issueDescription.replace(",", " ") + "," +
                photoPath.replace(",", " ") + "," +
                status + "," +
                (assignedTo == null ? "" : assignedTo) + "," +
                getPriority() + "," +
                createdAt.format(formatter) + "," +
                (completedAt == null ? "" : completedAt.format(formatter)) + "," +
                (rating == null ? "" : rating);
    }

    @Override
    public String toString() {
        return "ID=" + requestId +
                ", Room=" + roomNumber +
                ", Category=" + getCategory() +
                ", Status=" + status +
                ", Priority=" + getPriority() +
                ", AssignedTo=" + (assignedTo == null ? "-" : assignedTo) +
                ", Rating=" + (rating == null ? "-" : rating);
    }
}

