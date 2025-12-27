package com.rsvp.service;

import com.rsvp.dto.EventResponse;
import com.rsvp.dto.GroupRSVPSubmission;
import com.rsvp.entity.Event;
import com.rsvp.entity.Guest;
import com.rsvp.entity.RSVP;
import com.rsvp.repository.EventRepository;
import com.rsvp.repository.GuestRepository;
import com.rsvp.repository.RSVPRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GroupRSVPService {

    private final EventRepository eventRepository;
    private final GuestRepository guestRepository;
    private final RSVPRepository rsvpRepository;

    public GroupRSVPService(EventRepository eventRepository,
                            GuestRepository guestRepository,
                            RSVPRepository rsvpRepository) {
        this.eventRepository = eventRepository;
        this.guestRepository = guestRepository;
        this.rsvpRepository = rsvpRepository;
    }

    public Event getPublicEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .filter(event -> event.getStatus() == Event.EventStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Event not found or is closed"));
    }

    public EventResponse convertToResponse(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setVenue(event.getVenue());
        response.setEventDateTime(event.getEventDateTime());
        response.setRsvpDeadline(event.getRsvpDeadline());
        response.setBannerImage(event.getBannerImage());
        response.setStatus(event.getStatus());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());

        if (event.getOwner() != null) {
            response.setOwnerId(event.getOwner().getId());
            response.setOwnerName(event.getOwner().getFullName());
        }

        return response;
    }

    // Remove @Transactional from this method - let each submission have its own transaction
    public void syncGroupRSVPs(List<GroupRSVPSubmission> submissions) {
        System.out.println("🔄 Processing " + submissions.size() + " submissions");

        int successCount = 0;
        int failCount = 0;

        for (GroupRSVPSubmission submission : submissions) {
            try {
                // Process each submission in its own transaction
                processSubmission(submission);
                successCount++;
            } catch (Exception e) {
                failCount++;
                System.err.println("❌ Failed to process submission: " + submission.getFullName());
                System.err.println("   Error: " + e.getMessage());
                e.printStackTrace();
                // Continue with next submission instead of failing all
            }
        }

        System.out.println("\n🎉 Finished processing: " + successCount + " successful, " + failCount + " failed\n");
    }

    // Each submission gets its own transaction with REQUIRES_NEW
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSubmission(GroupRSVPSubmission submission) {
        System.out.println("\n📝 Processing submission:");
        System.out.println("  - Submission ID: " + submission.getId());
        System.out.println("  - Event ID: " + submission.getEventId());
        System.out.println("  - Full Name: " + submission.getFullName());
        System.out.println("  - Status: " + submission.getRsvpStatus());

        // Check if RSVP already exists by submission ID
        Optional<RSVP> existingRsvp = rsvpRepository.findBySubmissionId(submission.getId());

        if (existingRsvp.isPresent()) {
            System.out.println("ℹ️ RSVP already synced: " + submission.getFullName() + " (Submission ID: " + submission.getId() + ")");
            return;
        }

        // Convert eventId to Long
        Long eventId;
        try {
            eventId = Long.valueOf(submission.getEventId());
            System.out.println("  - Converted eventId to Long: " + eventId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid event ID format: " + submission.getEventId());
        }

        // Find event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        System.out.println("  - Event found: " + event.getName());

        // Try to find existing guest by event and name
        Optional<Guest> existingGuest = guestRepository.findByEventIdAndName(
                eventId,
                submission.getFullName()
        );

        // Create or get guest
        Guest guest;
        if (existingGuest.isEmpty()) {
            // Create new guest for walk-in
            guest = new Guest();
            guest.setGuestId(UUID.randomUUID().toString());
            guest.setEvent(event);
            guest.setName(submission.getFullName());
            guest.setGroupName("Walk-in");
            guest.setMaxInvitees(1);
            guest.setImportedFromFile(false);
            guest = guestRepository.save(guest);
            System.out.println("✅ Created new guest with ID: " + guest.getId() + " - " + submission.getFullName());
        } else {
            guest = existingGuest.get();
            System.out.println("ℹ️ Using existing guest with ID: " + guest.getId() + " - " + submission.getFullName());
        }

        // Determine RSVP status
        RSVP.RsvpStatus rsvpStatus;
        String statusStr = submission.getRsvpStatus();
        if ("YES".equalsIgnoreCase(statusStr) || "CONFIRMED".equalsIgnoreCase(statusStr)) {
            rsvpStatus = RSVP.RsvpStatus.YES;
        } else if ("NO".equalsIgnoreCase(statusStr)) {
            rsvpStatus = RSVP.RsvpStatus.NO;
        } else {
            System.out.println("⚠️ Unknown RSVP status: '" + statusStr + "', defaulting to YES");
            rsvpStatus = RSVP.RsvpStatus.YES;
        }

        // Create RSVP record
        RSVP rsvp = new RSVP();
        // Don't set ID - it's auto-generated
        rsvp.setSubmissionId(submission.getId());
        rsvp.setEvent(event);
        rsvp.setGuest(guest);
        rsvp.setGuestName(submission.getFullName());
        rsvp.setGroupName(guest.getGroupName());
        rsvp.setRsvpStatus(rsvpStatus);
        rsvp.setGuestsCount(1);
        rsvp.setSource(RSVP.Source.WEB_DIRECT);
        rsvp.setSubmissionTimestamp(LocalDateTime.now());
        rsvp.setSyncStatus(RSVP.SyncStatus.SYNCED);
        rsvp.setSyncedAt(LocalDateTime.now());

        // Save the RSVP
        RSVP savedRsvp = rsvpRepository.save(rsvp);
        System.out.println("✅ Saved RSVP with ID: " + savedRsvp.getId() + " for: " + submission.getFullName() + " (Status: " + rsvpStatus + ")");
    }



    @Transactional
    public Map<String, Object> submitGroupRSVP(GroupRSVPSubmission request) {
        System.out.println("\n📝 Submitting group RSVP:");
        System.out.println("  - Event ID: " + request.getEventId());
        System.out.println("  - Guest Name: " + request.getGuestName());
        System.out.println("  - RSVP Status: " + request.getRsvpStatus());
        System.out.println("  - Guests Count: " + request.getGuestsCount());

        // Convert eventId to Long
        Long eventId;
        try {
            eventId = Long.valueOf(request.getEventId());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid event ID format: " + request.getEventId());
        }

        // Find event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        // Use guestName from web or fullName from WhatsApp
        String name = request.getGuestName() != null ? request.getGuestName() : request.getFullName();

        // Create new guest record
        Guest guest = new Guest();
        guest.setGuestId(UUID.randomUUID().toString());
        guest.setName(name);
        guest.setEvent(event);
        guest.setMaxInvitees(1);
        guest.setGroupName("Walk-in");  // ✅ Changed to Walk-in
        guest.setImportedFromFile(false);
        guest.setCreatedAt(LocalDateTime.now());

        Guest savedGuest = guestRepository.save(guest);
        System.out.println("✅ Created guest with ID: " + savedGuest.getGuestId());

        // Create new RSVP
        RSVP rsvp = new RSVP();
        rsvp.setSubmissionId(UUID.randomUUID().toString());
        rsvp.setEvent(event);
        rsvp.setGuest(savedGuest);
        rsvp.setGuestName(name);
        rsvp.setGroupName("Walk-in");  // ✅ Changed to Walk-in
        rsvp.setRsvpStatus(RSVP.RsvpStatus.valueOf(request.getRsvpStatus()));
        rsvp.setGuestsCount(request.getGuestsCount() != null ? request.getGuestsCount() : 1);
        rsvp.setSource(RSVP.Source.WEB_DIRECT);  // ✅ Use existing WEB_DIRECT
        rsvp.setSubmissionTimestamp(LocalDateTime.now());
        rsvp.setSyncStatus(RSVP.SyncStatus.SYNCED);
        rsvp.setSyncedAt(LocalDateTime.now());

        RSVP savedRsvp = rsvpRepository.save(rsvp);
        System.out.println("✅ Created RSVP with ID: " + savedRsvp.getId());

        // Return response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("submissionId", savedRsvp.getSubmissionId());
        response.put("guestId", savedGuest.getGuestId());
        response.put("rsvpStatus", savedRsvp.getRsvpStatus().toString());
        response.put("guestsCount", savedRsvp.getGuestsCount());

        return response;
    }

    }