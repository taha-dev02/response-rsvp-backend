package com.rsvp.controller;

import com.rsvp.dto.EventDashboardResponse;
import com.rsvp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/event/{eventId}")
    public ResponseEntity<EventDashboardResponse> getEventDashboard(@PathVariable Long eventId) {
        EventDashboardResponse response = dashboardService.getEventDashboard(eventId);
        return ResponseEntity.ok(response);
    }
}