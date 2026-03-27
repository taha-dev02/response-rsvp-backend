package com.rsvp.controller;

import com.rsvp.dto.WhatsAppSyncRequest;
import com.rsvp.dto.WhatsAppSyncResponse;
import com.rsvp.entity.WhatsAppLog;
import com.rsvp.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/whatsapp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String FLASK_SERVER_URL = "http://localhost:5000";

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

    @PostMapping("/trigger-automation")
    public ResponseEntity<Map<String, Object>> triggerWhatsAppAutomation(@RequestBody Map<String, Object> request) {
        try {
            // Forward the request to Flask server
            String flaskUrl = FLASK_SERVER_URL + "/api/automation/start";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                flaskUrl, 
                HttpMethod.POST, 
                entity, 
                Map.class
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to trigger automation: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/automation-status/{automationId}")
    public ResponseEntity<Map<String, Object>> getAutomationStatus(@PathVariable String automationId) {
        try {
            // Get status from Flask server
            String flaskUrl = FLASK_SERVER_URL + "/api/automation/status/" + automationId;
            
            ResponseEntity<Map> response = restTemplate.getForEntity(flaskUrl, Map.class);
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get automation status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}