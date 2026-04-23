package com.hosteltracker.model.request;

import com.hosteltracker.model.enums.Priority;

public class PlumbingRequest extends Request {
    public PlumbingRequest(String roomNumber, String issueDescription, String photoPath) {
        super(roomNumber, issueDescription, photoPath);
    }

    @Override
    public Priority getPriority() {
        String issue = getIssueDescription().toLowerCase();
        if (issue.contains("leak") || issue.contains("flood") || issue.contains("burst")) {
            return Priority.URGENT;
        }
        return Priority.ROUTINE;
    }

    @Override
    public String getCategory() {
        return "Plumbing";
    }
}

