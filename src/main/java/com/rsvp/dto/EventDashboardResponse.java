package com.rsvp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDashboardResponse {
    private Long eventId;
    private String eventName;
    private LocalDateTime eventDateTime;
    private String venue;
    private EventMetrics metrics;
    private List<GuestTableRow> guestTable;
    private List<GroupSummary> groupSummaries;
}