package com.rsvp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RSVPSubmission {
    private String submissionId;
    private Long eventId;
    private String guestId;
    private String guestName;
    private String guestAlias;
    private String groupName;
    private String rsvpStatus; // YES / NO
    private Integer guestsCount;
    private LocalDateTime submissionTimestamp;
    private String source;
    private String notes;

        // Getters and Setters
        public Long getEventId() { return eventId; }
        public void setEventId(Long eventId) { this.eventId = eventId; }

        public String getGuestId() { return guestId; }
        public void setGuestId(String guestId) { this.guestId = guestId; }

        public String getGuestName() { return guestName; }
        public void setGuestName(String guestName) { this.guestName = guestName; }

        public String getRsvpStatus() { return rsvpStatus; }
        public void setRsvpStatus(String rsvpStatus) { this.rsvpStatus = rsvpStatus; }

        public Integer getGuestsCount() { return guestsCount; }
        public void setGuestsCount(Integer guestsCount) { this.guestsCount = guestsCount; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }

}