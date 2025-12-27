package com.rsvp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventMetrics {
    private Long totalInvites;
    private Long yesCount;
    private Long noCount;
    private Long pendingCount;
    private Long totalAttending;
    private Double responseRate;
    private Long whatsappSent;
    private Long whatsappFailed;
    private Long linkClicks;
    private Double clickRate;
    private Long clickedButNotResponded;

}

