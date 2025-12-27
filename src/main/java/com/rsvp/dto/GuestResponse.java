package com.rsvp.dto;

import com.rsvp.entity.Guest;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;


@Data
public class GuestResponse {
    private Long id;
    private String guestId;
    private String name;
    private String phone;
    private Integer maxInvitees;
    private String groupName;
    private List<String> aliases;
    private Boolean importedFromFile;
    private String importBatchId;
    private String importFileName;
    private LocalDateTime importedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long eventId;
    private String eventName;
    private String eventDateTime;
    private String eventTime;
    private String eventLocation;
    private String eventDescription;

    public static GuestResponse fromEntity(Guest guest) {
        GuestResponse response = new GuestResponse();
        response.setId(guest.getId());
        response.setGuestId(guest.getGuestId());
        response.setName(guest.getName());
        response.setPhone(guest.getPhone());
        response.setMaxInvitees(guest.getMaxInvitees());
        response.setGroupName(guest.getGroupName());
        response.setAliases(guest.getAliases());
        response.setImportedFromFile(guest.getImportedFromFile());
        response.setImportBatchId(guest.getImportBatchId());
        response.setImportFileName(guest.getImportFileName());
        response.setImportedAt(guest.getImportedAt());
        response.setCreatedAt(guest.getCreatedAt());
        response.setUpdatedAt(guest.getUpdatedAt());


        if (guest.getEvent() != null) {
            response.setEventId(guest.getEvent().getId());
            response.setEventName(guest.getEvent().getName());
            response.setEventDateTime(String.valueOf(guest.getEvent().getEventDateTime()));
            response.setEventLocation(guest.getEvent().getVenue());
            response.setEventDescription(guest.getEvent().getDescription());
        }

        return response;
    }
}
