package com.rsvp.dto;

import lombok.Data;
import java.util.List;

@Data
public class GroupRSVPSyncRequest {
    private List<GroupRSVPSubmission> submissions;
}