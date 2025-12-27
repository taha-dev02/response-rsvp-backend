// com/rsvp/dto/EventRequest.java
package com.rsvp.dto;

import com.rsvp.entity.Event;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventRequest {
    private String name;
    private String description;
    private String venue;
    private LocalDateTime eventDateTime;
    private LocalDateTime rsvpDeadline;
    private String bannerImage;
    private Event.EventStatus status;
}