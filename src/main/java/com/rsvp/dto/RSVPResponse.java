package com.rsvp.dto;

import com.rsvp.entity.RSVP;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RSVPResponse {
    private Long id;
    private String submissionId;
    private Long eventId;
    private String eventName;
    private Long guestId;
    private String guestName;
    private String guestAlias;
    private String groupName;
    private RSVP.RsvpStatus rsvpStatus;
    private Integer guestsCount;
    private RSVP.Source source;
    private LocalDateTime submissionTimestamp;
    private RSVP.SyncStatus syncStatus;
    private LocalDateTime syncedAt;
    private String notes;

    public static RSVPResponse fromEntity(RSVP rsvp) {
        RSVPResponse response = new RSVPResponse();
        response.setId(rsvp.getId());
        response.setSubmissionId(rsvp.getSubmissionId());
        response.setEventId(rsvp.getEvent().getId());
        response.setEventName(rsvp.getEvent().getName());
        if (rsvp.getGuest() != null) {
            response.setGuestId(rsvp.getGuest().getId());
            response.setGuestName(rsvp.getGuest().getName());
        } else {
            response.setGuestName(rsvp.getGuestName());
        }
        response.setGuestAlias(rsvp.getGuestAlias());
        response.setGroupName(rsvp.getGroupName());
        response.setRsvpStatus(rsvp.getRsvpStatus());
        response.setGuestsCount(rsvp.getGuestsCount());
        response.setSource(rsvp.getSource());
        response.setSubmissionTimestamp(rsvp.getSubmissionTimestamp());
        response.setSyncStatus(rsvp.getSyncStatus());
        response.setSyncedAt(rsvp.getSyncedAt());
        response.setNotes(rsvp.getNotes());
        return response;
    }
}