package com.rsvp.dto;

import lombok.Data;
import java.util.List;

@Data
public class RSVPSyncRequest {
    private List<RSVPSubmission> submissions;
}

