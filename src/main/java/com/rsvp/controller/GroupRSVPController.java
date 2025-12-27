package com.rsvp.controller;

import com.rsvp.dto.EventResponse;
import com.rsvp.dto.GroupRSVPSubmission;
import com.rsvp.dto.GroupRSVPSyncRequest;
import com.rsvp.entity.Event;
import com.rsvp.service.GroupRSVPService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class GroupRSVPController {

    private final GroupRSVPService groupRSVPService;

    public GroupRSVPController(GroupRSVPService groupRSVPService) {
        this.groupRSVPService = groupRSVPService;
    }

    @GetMapping("/events/{id}/public")
    public ResponseEntity<EventResponse> getEventPublic(@PathVariable Long id) {
        Event event = groupRSVPService.getPublicEvent(id);
        EventResponse response = groupRSVPService.convertToResponse(event);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rsvps/submit-group")
    public ResponseEntity<?> submitGroupRSVP(@RequestBody GroupRSVPSubmission request) {
        try {
            System.out.println("\n========== GROUP RSVP SUBMISSION ==========");
            System.out.println("Event ID: " + request.getEventId());
            System.out.println("Guest Name: " + request.getGuestName());
            System.out.println("RSVP Status: " + request.getRsvpStatus());
            System.out.println("Guests Count: " + request.getGuestsCount());

            if (request.getEventId() == null || request.getGuestName() == null ||
                    request.getRsvpStatus() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Missing required fields: eventId, guestName, rsvpStatus"
                ));
            }

            Map<String, Object> result = groupRSVPService.submitGroupRSVP(request);

            System.out.println("✅ Group RSVP submitted successfully");
            System.out.println("===========================================\n");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("\n========== ERROR IN GROUP RSVP SUBMISSION ==========");
            e.printStackTrace();
            System.err.println("====================================================\n");

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    @PostMapping("/rsvps/group-sync")
    public ResponseEntity<?> syncGroupRSVPs(@RequestBody GroupRSVPSyncRequest request) {
        try {
            System.out.println("\n========== GROUP SYNC REQUEST ==========");

            if (request.getSubmissions() == null || request.getSubmissions().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "No submissions provided"
                ));
            }

            List<GroupRSVPSubmission> submissions = request.getSubmissions();
            System.out.println("Number of submissions: " + submissions.size());

            groupRSVPService.syncGroupRSVPs(submissions);

            System.out.println("✅ Sync completed successfully");
            System.out.println("========================================\n");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "synced", submissions.size()
            ));

        } catch (Exception e) {
            System.err.println("\n========== ERROR IN GROUP SYNC ==========");
            e.printStackTrace();
            System.err.println("=========================================\n");

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    // Helper method to convert Map to GroupRSVPSubmission
    private GroupRSVPSubmission convertToSubmission(Map<String, Object> map) {
        GroupRSVPSubmission submission = new GroupRSVPSubmission();

        submission.setId((String) map.get("id"));
        submission.setEventId(String.valueOf(map.get("eventId"))); // Convert to String
        submission.setFullName((String) map.get("fullName"));
        submission.setRsvpStatus((String) map.get("rsvpStatus"));
        submission.setSubmittedAt((String) map.get("submittedAt"));
        submission.setSynced((Boolean) map.getOrDefault("synced", false));

        System.out.println("Converted submission: " + submission.getFullName() +
                " for event " + submission.getEventId());

        return submission;
    }
}