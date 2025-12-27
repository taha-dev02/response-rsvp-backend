package com.rsvp.controller;

import com.rsvp.dto.*;
import com.rsvp.entity.*;
import com.rsvp.repository.GuestRepository;
import com.rsvp.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guests")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GuestController {

    private final GuestService guestService;
    private final GuestRepository guestRepository;

    @PostMapping
    public ResponseEntity<GuestResponse> createGuest(@RequestBody GuestRequest request) {
        GuestResponse response = guestService.createGuest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GuestResponse> updateGuest(
            @PathVariable Long id,
            @RequestBody GuestRequest request) {
        GuestResponse response = guestService.updateGuest(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuestResponse> getGuest(@PathVariable Long id) {
        GuestResponse response = guestService.getGuest(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-guest-id/{guestId}")
    public ResponseEntity<?> getGuestByGuestId(@PathVariable String guestId) {
        try {
            System.out.println("\n========== GET GUEST BY GUEST ID ==========");
            System.out.println("Guest ID: " + guestId);

            Guest guest = guestRepository.findByGuestId(guestId)
                    .orElseThrow(() -> new RuntimeException("Guest not found: " + guestId));

            System.out.println("✅ Found guest: " + guest.getName());
            System.out.println("   Event ID: " + guest.getEvent().getId());
            System.out.println("   Max Invitees: " + guest.getMaxInvitees());
            System.out.println("==========================================\n");

            // Return guest data with eventId
            Map<String, Object> response = new HashMap<>();
            response.put("id", guest.getId());
            response.put("guestId", guest.getGuestId());
            response.put("name", guest.getName());
            response.put("phone", guest.getPhone());
            response.put("maxInvitees", guest.getMaxInvitees());
            response.put("groupName", guest.getGroupName());
            response.put("aliases", guest.getAliases());
            response.put("eventId", guest.getEvent().getId()); // Important!

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("\n========== ERROR GETTING GUEST ==========");
            e.printStackTrace();
            System.err.println("=========================================\n");

            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "error", e.getMessage() != null ? e.getMessage() : "Guest not found"
            ));
        }
    }

    @GetMapping
    public ResponseEntity<List<GuestResponse>> getAllGuests(
            @RequestParam(required = false) String groupName) {
        List<GuestResponse> guests;
        if (groupName != null) {
            guests = guestService.getGuestsByGroup(groupName);
        } else {
            guests = guestService.getAllGuests();
        }
        return ResponseEntity.ok(guests);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<String>> getAllGroupNames() {
        List<String> groups = guestService.getAllGroupNames();
        return ResponseEntity.ok(groups);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuest(@PathVariable Long id) {
        guestService.deleteGuest(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<GuestImportResponse> importGuests(
            @RequestParam Long eventId,  // Change from @PathVariable to @RequestParam
            @RequestBody GuestImportRequest request
    ) {
        GuestImportResponse response = guestService.importGuests(request, eventId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



}