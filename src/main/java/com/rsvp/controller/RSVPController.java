package com.rsvp.controller;


import com.rsvp.dto.RSVPSubmission;
import com.rsvp.entity.Event;
import com.rsvp.entity.Guest;
import com.rsvp.entity.RSVP;
import com.rsvp.repository.EventRepository;
import com.rsvp.repository.GuestRepository;
import com.rsvp.repository.RSVPRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/rsvps")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class RSVPController {

    private final RSVPRepository rsvpRepository;
    private final EventRepository eventRepository;
    private final GuestRepository guestRepository;

    public RSVPController(RSVPRepository rsvpRepository,
                          EventRepository eventRepository,
                          GuestRepository guestRepository) {
        this.rsvpRepository = rsvpRepository;
        this.eventRepository = eventRepository;
        this.guestRepository = guestRepository;
    }

    /**
     * PUBLIC endpoint to submit RSVP from public RSVP page
     * POST /api/rsvps/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitPublicRSVP(@RequestBody RSVPSubmission request) {
        try {
            System.out.println("\n========== PUBLIC RSVP SUBMISSION ==========");
            System.out.println("Event ID: " + request.getEventId());
            System.out.println("Guest ID: " + request.getGuestId());
            System.out.println("Guest Name: " + request.getGuestName());
            System.out.println("RSVP Status: " + request.getRsvpStatus());
            System.out.println("Guests Count: " + request.getGuestsCount());

            // Validate required fields
            if (request.getEventId() == null || request.getGuestId() == null ||
                    request.getRsvpStatus() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Missing required fields"
                ));
            }

            // Find event
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            // Find guest by guestId (not database ID)
            Guest guest = guestRepository.findByGuestId(request.getGuestId())
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            // Check if already submitted
            List<RSVP> existingRsvps = rsvpRepository.findByEventAndGuest(event, guest);
            if (!existingRsvps.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "RSVP already submitted"
                ));
            }

            // Create new RSVP
            RSVP rsvp = new RSVP();
            rsvp.setSubmissionId(UUID.randomUUID().toString());
            rsvp.setEvent(event);
            rsvp.setGuest(guest);
            rsvp.setGuestName(request.getGuestName());
            rsvp.setGroupName(guest.getGroupName());

            // Parse RSVP status
            RSVP.RsvpStatus status = RSVP.RsvpStatus.valueOf(request.getRsvpStatus());
            rsvp.setRsvpStatus(status);

            rsvp.setGuestsCount(request.getGuestsCount() != null ? request.getGuestsCount() : 0);
            rsvp.setSource(RSVP.Source.WEB_DIRECT);
            rsvp.setSubmissionTimestamp(LocalDateTime.now());
            rsvp.setSyncStatus(RSVP.SyncStatus.SYNCED);
            rsvp.setSyncedAt(LocalDateTime.now());

            // Save RSVP
            RSVP savedRsvp = rsvpRepository.save(rsvp);

            System.out.println("✅ RSVP saved successfully with ID: " + savedRsvp.getId());
            System.out.println("===========================================\n");

            // Return response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("submissionId", savedRsvp.getSubmissionId());
            response.put("rsvpStatus", savedRsvp.getRsvpStatus().toString());
            response.put("guestsCount", savedRsvp.getGuestsCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("\n========== ERROR IN RSVP SUBMISSION ==========");
            e.printStackTrace();
            System.err.println("==============================================\n");

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }





    /**
     * PUBLIC endpoint to check if guest already submitted RSVP
     * GET /api/rsvps/event/{eventId}/guest-id/{guestId}/latest
     */
    @GetMapping("/event/{eventId}/guest-id/{guestId}/latest")
    public ResponseEntity<?> getLatestRSVP(@PathVariable Long eventId,
                                           @PathVariable String guestId) {
        try {
            System.out.println("\n========== CHECK EXISTING RSVP ==========");
            System.out.println("Event ID: " + eventId);
            System.out.println("Guest ID: " + guestId);

            // Find event
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new RuntimeException("Event not found"));

            // Find guest by guestId
            Guest guest = guestRepository.findByGuestId(guestId)
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            // Get latest RSVP
            List<RSVP> rsvps = rsvpRepository.findLatestByEventAndGuest(event, guest);

            if (rsvps.isEmpty()) {
                System.out.println("❌ No existing RSVP found");
                System.out.println("========================================\n");
                return ResponseEntity.notFound().build();
            }

            RSVP latestRsvp = rsvps.get(0);

            System.out.println("✅ Found existing RSVP: " + latestRsvp.getRsvpStatus());
            System.out.println("========================================\n");

            // Return RSVP data
            Map<String, Object> response = new HashMap<>();
            response.put("submissionId", latestRsvp.getSubmissionId());
            response.put("rsvpStatus", latestRsvp.getRsvpStatus().toString());
            response.put("guestsCount", latestRsvp.getGuestsCount());
            response.put("submissionTimestamp", latestRsvp.getSubmissionTimestamp().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("\n========== ERROR CHECKING RSVP ==========");
            e.printStackTrace();
            System.err.println("=========================================\n");
            return ResponseEntity.notFound().build();
        }
    }
}

// DTO class for RSVP submission
