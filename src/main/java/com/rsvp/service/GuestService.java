package com.rsvp.service;

import com.rsvp.dto.*;
import com.rsvp.entity.Event;
import com.rsvp.entity.Guest;
import com.rsvp.entity.ImportBatch;
import com.rsvp.exception.ResourceNotFoundException;
import com.rsvp.exception.ValidationException;
import com.rsvp.repository.EventRepository;
import com.rsvp.repository.GuestRepository;
import com.rsvp.repository.ImportBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final ImportBatchRepository importBatchRepository;
    private final EventRepository eventRepository;

    @Transactional
    public GuestResponse createGuest(GuestRequest request) {

        Guest guest = new Guest();
        guest.setGuestId(UUID.randomUUID().toString());
        guest.setName(request.getName());
        guest.setPhone(request.getPhone());
        guest.setMaxInvitees(request.getMaxInvitees() != null ? request.getMaxInvitees() : 1);
        guest.setGroupName(request.getGroupName());
        guest.setAliases(request.getAliases() != null ? request.getAliases() : new ArrayList<>());
        guest.setImportedFromFile(false);

        Guest savedGuest = guestRepository.save(guest);
        return GuestResponse.fromEntity(savedGuest);
    }

    @Transactional
    public GuestResponse updateGuest(Long id, GuestRequest request) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + id));

        if (request.getName() != null) guest.setName(request.getName());
        if (request.getPhone() != null) guest.setPhone(request.getPhone());
        if (request.getMaxInvitees() != null) guest.setMaxInvitees(request.getMaxInvitees());
        if (request.getGroupName() != null) guest.setGroupName(request.getGroupName());
        if (request.getAliases() != null) guest.setAliases(request.getAliases());


        Guest updatedGuest = guestRepository.save(guest);
        return GuestResponse.fromEntity(updatedGuest);
    }

    @Transactional(readOnly = true)
    public GuestResponse getGuest(Long id) {
        Guest guest = guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + id));
        return GuestResponse.fromEntity(guest);
    }

    @Transactional(readOnly = true)
    public GuestResponse getGuestByGuestId(String guestId) {
        Guest guest = guestRepository.findByGuestId(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with guestId: " + guestId));
        return GuestResponse.fromEntity(guest);
    }

    @Transactional(readOnly = true)
    public List<GuestResponse> getAllGuests() {
        return guestRepository.findAll().stream()
                .map(GuestResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GuestResponse> getGuestsByGroup(String groupName) {
        return guestRepository.findByGroupName(groupName).stream()
                .map(GuestResponse::fromEntity)
                .collect(Collectors.toList());
    }


    @Transactional
    public void deleteGuest(Long id) {
        if (!guestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Guest not found with id: " + id);
        }
        guestRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<String> getAllGroupNames() {
        return guestRepository.findAllGroupNames();
    }


    @Transactional
    public GuestImportResponse importGuests(GuestImportRequest request, Long eventId) {
        // NEW: Fetch event first
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !event.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("You do not have permission to import guests for this event");
        }
        String batchId = UUID.randomUUID().toString();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        ImportBatch batch = new ImportBatch();
        batch.setBatchId(batchId);
        batch.setFileName(request.getFileName());
        batch.setFileType(request.getFileType());
        batch.setTotalRows(request.getGuests().size());
        batch.setImportedBy(request.getImportedBy());
        batch.setStatus(ImportBatch.ImportStatus.IN_PROGRESS);
        batch = importBatchRepository.save(batch);

        for (GuestImportRow row : request.getGuests()) {
            try {
                validateImportRow(row);

                Guest guest = new Guest();
                guest.setGuestId(UUID.randomUUID().toString());
                guest.setEvent(event);  // ADD THIS - Set the event
                guest.setName(row.getName());
                guest.setPhone(row.getPhone());
                guest.setMaxInvitees(row.getMaxInvitees() != null ? row.getMaxInvitees() : 1);
                guest.setGroupName(row.getGroupName());
                guest.setAliases(row.getAliases() != null ? row.getAliases() : new ArrayList<>());
                guest.setImportedFromFile(true);
                guest.setImportBatchId(batchId);
                guest.setImportFileName(request.getFileName());
                guest.setImportedAt(LocalDateTime.now());
                guest.setImportedBy(request.getImportedBy());

                guestRepository.save(guest);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add("Row " + row.getRowNumber() + ": " + e.getMessage());
            }
        }

        batch.setSuccessfulImports(successCount);
        batch.setFailedImports(failCount);
        batch.setStatus(failCount > 0 ? ImportBatch.ImportStatus.PARTIAL_SUCCESS : ImportBatch.ImportStatus.COMPLETED);
        batch.setCompletedAt(LocalDateTime.now());
        if (!errors.isEmpty()) {
            batch.setErrorLog(String.join("\n", errors));
        }
        importBatchRepository.save(batch);

        GuestImportResponse response = new GuestImportResponse();
        response.setBatchId(batchId);
        response.setTotalRows(request.getGuests().size());
        response.setSuccessfulImports(successCount);
        response.setFailedImports(failCount);
        response.setErrors(errors);
        response.setStatus(batch.getStatus().name());

        return response;
    }



    private void validateImportRow(GuestImportRow row) {
        // Validate name
        if (row.getName() == null || row.getName().trim().isEmpty()) {
            throw new ValidationException("Name is required");
        }

        // Validate phone - now mandatory
        if (row.getPhone() == null || row.getPhone().trim().isEmpty()) {
            throw new ValidationException("Phone number is required");
        }

        // Optional: Validate phone format (basic check)
        if (!row.getPhone().matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new ValidationException("Invalid phone number format");
        }
    }




}