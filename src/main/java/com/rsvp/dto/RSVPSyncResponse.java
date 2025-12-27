package com.rsvp.dto;

import lombok.Data;
import java.util.List;

@Data
public class RSVPSyncResponse {
    private Integer totalReceived;
    private Integer successfulSyncs;
    private Integer failedSyncs;
    private Integer duplicates;
    private List<String> errors;
}