// com/rsvp/dto/EventResponse.java
package com.rsvp.dto;

import com.rsvp.entity.Event;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private String venue;
    private LocalDateTime eventDateTime;
    private LocalDateTime rsvpDeadline;
    private String bannerImage;
    private Event.EventStatus status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 🔹 Add owner information
    private Long ownerId;
    private String ownerName;
}