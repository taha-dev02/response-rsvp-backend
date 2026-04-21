package com.rsvp.controller;

import com.rsvp.entity.AutomationRequest;
import com.rsvp.service.AutomationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/automation")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AutomationController {

    private final AutomationService automationService;

    /**
     * POST /api/automation/request/{eventId}?eventName=...
     * Creates a new automation approval request and sends email to admin.
     */
    @PostMapping("/request/{eventId}")
    public ResponseEntity<Map<String, Object>> createRequest(
            @PathVariable Long eventId,
            @RequestParam String eventName) {

        log.info("Received automation request for event '{}' (id={})", eventName, eventId);

        AutomationRequest request = automationService.createRequest(eventId, eventName);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Automation request created. Approval email sent to admin.");
        response.put("requestId", request.getId());
        response.put("status", request.getStatus().name());

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/automation/approve/{requestId}
     * Admin approves the automation request.
     */
    @PutMapping("/approve/{requestId}")
    public ResponseEntity<Map<String, Object>> approveRequest(@PathVariable Long requestId) {
        log.info("Approving automation request #{}", requestId);

        AutomationRequest request = automationService.approveRequest(requestId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Automation request approved successfully.");
        response.put("requestId", request.getId());
        response.put("status", request.getStatus().name());

        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/automation/reject/{requestId}
     * Admin rejects the automation request.
     */
    @PutMapping("/reject/{requestId}")
    public ResponseEntity<Map<String, Object>> rejectRequest(@PathVariable Long requestId) {
        log.info("Rejecting automation request #{}", requestId);

        AutomationRequest request = automationService.rejectRequest(requestId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Automation request rejected.");
        response.put("requestId", request.getId());
        response.put("status", request.getStatus().name());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/automation/status/{requestId}
     * Returns the current status of an automation request.
     * This endpoint is public (no auth required) — used by Python polling and approval page.
     */
    @GetMapping("/status/{requestId}")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable Long requestId) {
        AutomationRequest request = automationService.getStatus(requestId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("requestId", request.getId());
        response.put("eventId", request.getEventId());
        response.put("eventName", request.getEventName());
        response.put("status", request.getStatus().name());
        response.put("requestedAt", request.getRequestedAt());
        response.put("approvedAt", request.getApprovedAt());

        return ResponseEntity.ok(response);
    }
}
