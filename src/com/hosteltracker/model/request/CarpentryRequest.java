package com.hosteltracker.model.request;

import com.hosteltracker.model.enums.Priority;

public class CarpentryRequest extends Request {
    public CarpentryRequest(String roomNumber, String issueDescription, String photoPath) {
        super(roomNumber, issueDescription, photoPath);
    }

    @Override
    public Priority getPriority() {
        String issue = getIssueDescription().toLowerCase();
        if (issue.contains("broken door") || issue.contains("jammed") || issue.contains("unsafe")) {
            return Priority.URGENT;
        }
        return Priority.ROUTINE;
    }

    @Override
    public String getCategory() {
        return "Carpentry";
    }
}

