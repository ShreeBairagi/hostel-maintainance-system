package com.hosteltracker.model.interfaces;

import com.hosteltracker.model.enums.RequestStatus;

public interface Trackable {
    RequestStatus getStatus();
    void updateStatus(RequestStatus newStatus);
}

