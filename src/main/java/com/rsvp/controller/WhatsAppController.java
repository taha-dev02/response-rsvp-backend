package com.rsvp.controller;

import com.rsvp.dto.WhatsAppSyncRequest;
import com.rsvp.dto.WhatsAppSyncResponse;
import com.rsvp.entity.WhatsAppLog;
import com.rsvp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @PostMapping("/sync")
    public ResponseEntity<WhatsAppSyncResponse> syncWhatsAppLogs(@RequestBody WhatsAppSyncRequest request) {
        WhatsAppSyncResponse response = whatsAppService.syncWhatsAppLogs(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<WhatsAppLog>> getLogsByEvent(@PathVariable Long eventId) {
        List<WhatsAppLog> logs = whatsAppService.getLogsByEvent(eventId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/guest/{guestId}")
    public ResponseEntity<List<WhatsAppLog>> getLogsByGuest(@PathVariable Long guestId) {
        List<WhatsAppLog> logs = whatsAppService.getLogsByGuest(guestId);
        return ResponseEntity.ok(logs);
    }
}