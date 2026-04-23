package com.hosteltracker.model.request;

import com.hosteltracker.model.enums.Priority;

public class ElectricalRequest extends Request {
    public ElectricalRequest(String roomNumber, String issueDescription, String photoPath) {
        super(roomNumber, issueDescription, photoPath);
    }

    @Override
    public Priority getPriority() {
        String issue = getIssueDescription().toLowerCase();
        if (issue.contains("spark") || issue.contains("smoke") || issue.contains("shock")) {
            return Priority.URGENT;
        }
        return Priority.ROUTINE;
    }

    @Override
    public String getCategory() {
        return "Electrical";
    }
}

