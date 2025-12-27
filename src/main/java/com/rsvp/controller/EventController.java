// com/rsvp/controller/EventController.java
package com.rsvp.controller;

import com.rsvp.dto.EventRequest;
import com.rsvp.dto.EventResponse;
import com.rsvp.entity.User;
import com.rsvp.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventResponse>> getAllEvents(
            @AuthenticationPrincipal User currentUser  // 🔹 Pass current user
    ) {
        return ResponseEntity.ok(eventService.getAllEvents(currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser  // 🔹 Pass current user
    ) {
        return ResponseEntity.ok(eventService.getEventById(id, currentUser));
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @RequestBody EventRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(eventService.createEvent(request, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @RequestBody EventRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, currentUser));
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<EventResponse> closeEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(eventService.closeEvent(id, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        eventService.deleteEvent(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}