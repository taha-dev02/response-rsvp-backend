package com.rsvp.dto;

import lombok.Data;

@Data
public class GroupRSVPSubmission {
    private String id;
    private String eventId;
    private String fullName;      // Keep for WhatsApp sync
    private String guestName;     // ✅ Add for web submission
    private String guestPhone;    // ✅ Add for web submission (optional)
    private String rsvpStatus;
    private Integer guestsCount;  // ✅ Add for web submission
    private String submittedAt;
    private Boolean synced;
}
