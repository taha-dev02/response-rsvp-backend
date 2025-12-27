package com.rsvp.service;

import com.rsvp.dto.*;
import com.rsvp.entity.*;
import com.rsvp.exception.ResourceNotFoundException;
import com.rsvp.exception.ValidationException;
import com.rsvp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RSVPService {

    private final RSVPRepository rsvpRepository;
    private final EventRepository eventRepository;
    private final GuestRepository guestRepository;

    @Transactional
    public RSVPSyncResponse syncRSVPs(RSVPSyncRequest request) {
        int totalReceived = request.getSubmissions().size();
        int successCount = 0;
        int failCount = 0;
        int duplicateCount = 0;
        List<String> errors = new ArrayList<>();

        for (RSVPSubmission submission : request.getSubmissions()) {
            try {
                // Check for duplicates
                Optional<RSVP> existing = rsvpRepository.findBySubmissionId(submission.getSubmissionId());
                if (existing.isPresent()) {
                    duplicateCount++;
                    log.info("Duplicate RSVP submission ignored: {}", submission.getSubmissionId());
                    continue;
                }

                // Validate and fetch event
                Event event = eventRepository.findById(submission.getEventId())
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + submission.getEventId()));

                // Try to resolve guest
                Guest guest = resolveGuest(submission);

                // Create RSVP
                RSVP rsvp = new RSVP();
                rsvp.setSubmissionId(submission.getSubmissionId());
                rsvp.setEvent(event);
                rsvp.setGuest(guest);
                rsvp.setGuestName(submission.getGuestName());
                rsvp.setGuestAlias(submission.getGuestAlias());
                rsvp.setGroupName(submission.getGroupName());
                rsvp.setRsvpStatus(RSVP.RsvpStatus.valueOf(submission.getRsvpStatus()));
                rsvp.setGuestsCount(submission.getGuestsCount() != null ? submission.getGuestsCount() : 1);
                rsvp.setSource(submission.getSource() != null ? RSVP.Source.valueOf(submission.getSource()) : RSVP.Source.WEB_DIRECT);
                rsvp.setSubmissionTimestamp(submission.getSubmissionTimestamp() != null ? submission.getSubmissionTimestamp() : LocalDateTime.now());
                rsvp.setSyncStatus(RSVP.SyncStatus.SYNCED);
                rsvp.setSyncedAt(LocalDateTime.now());
                rsvp.setNotes(submission.getNotes());

                rsvpRepository.save(rsvp);
                successCount++;

            } catch (Exception e) {
                failCount++;
                errors.add("Submission " + submission.getSubmissionId() + ": " + e.getMessage());
                log.error("Failed to sync RSVP: {}", submission.getSubmissionId(), e);
            }
        }

        RSVPSyncResponse response = new RSVPSyncResponse();
        response.setTotalReceived(totalReceived);
        response.setSuccessfulSyncs(successCount);
        response.setFailedSyncs(failCount);
        response.setDuplicates(duplicateCount);
        response.setErrors(errors);

        return response;
    }

    @Transactional(readOnly = true)
    public List<RSVPResponse> getRSVPsByEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        return rsvpRepository.findByEvent(event).stream()
                .map(RSVPResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RSVPResponse> getRSVPsByEventAndStatus(Long eventId, RSVP.RsvpStatus status) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        return rsvpRepository.findByEventAndRsvpStatus(event, status).stream()
                .map(RSVPResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RSVPResponse getLatestRSVPForGuest(Long eventId, Long guestId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + guestId));

        List<RSVP> rsvps = rsvpRepository.findLatestByEventAndGuest(event, guest);
        if (rsvps.isEmpty()) {
            throw new ResourceNotFoundException("No RSVP found for guest");
        }

        return RSVPResponse.fromEntity(rsvps.get(0));
    }

    private Guest resolveGuest(RSVPSubmission submission) {
        // Try by guestId first
        if (submission.getGuestId() != null && !submission.getGuestId().trim().isEmpty()) {
            Optional<Guest> guestOpt = guestRepository.findByGuestId(submission.getGuestId());
            if (guestOpt.isPresent()) {
                return guestOpt.get();
            }
        }

        // Try by alias
        if (submission.getGuestAlias() != null && !submission.getGuestAlias().trim().isEmpty()) {
            List<Guest> guestsByAlias = guestRepository.findByAlias(submission.getGuestAlias());
            if (!guestsByAlias.isEmpty()) {
                // If multiple matches, try to filter by group name if provided
                if (submission.getGroupName() != null && guestsByAlias.size() > 1) {
                    Optional<Guest> matchingGuest = guestsByAlias.stream()
                            .filter(g -> submission.getGroupName().equalsIgnoreCase(g.getGroupName()))
                            .findFirst();
                    if (matchingGuest.isPresent()) {
                        return matchingGuest.get();
                    }
                }
                return guestsByAlias.get(0);
            }
        }

        // Try by exact name match
        if (submission.getGuestName() != null && !submission.getGuestName().trim().isEmpty()) {
            Optional<Guest> guestByName = guestRepository.findByNameIgnoreCase(submission.getGuestName());
            if (guestByName.isPresent()) {
                return guestByName.get();
            }
        }

        // Guest not found - this is acceptable as frontend might submit without guest resolution
        log.warn("Could not resolve guest for submission: {}", submission.getSubmissionId());
        return null;
    }
}