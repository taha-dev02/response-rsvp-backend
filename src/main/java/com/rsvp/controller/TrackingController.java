package com.rsvp.controller;

import com.rsvp.entity.Event;
import com.rsvp.entity.Guest;
import com.rsvp.entity.RSVPLinkClick;
import com.rsvp.repository.GuestRepository;
import com.rsvp.repository.RSVPLinkClickRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
@CrossOrigin(origins = {"http://localhost:3000", "https://response-rsvp.vercel.app"})
public class TrackingController {

    private final RSVPLinkClickRepository linkClickRepository;
    private final GuestRepository guestRepository;

    public TrackingController(RSVPLinkClickRepository linkClickRepository,
                              GuestRepository guestRepository) {
        this.linkClickRepository = linkClickRepository;
        this.guestRepository = guestRepository;
    }

    /**
     * PUBLIC endpoint to track when a guest clicks their RSVP link
     * POST /api/tracking/link-click/{guestId}
     */
    @PostMapping("/link-click/{guestId}")
    public ResponseEntity<?> trackLinkClick(
            @PathVariable String guestId,
            HttpServletRequest request) {

        try {
            System.out.println("\n========== LINK CLICK TRACKING ==========");
            System.out.println("Guest ID: " + guestId);

            // Find guest
            Guest guest = guestRepository.findByGuestId(guestId)
                    .orElseThrow(() -> new RuntimeException("Guest not found"));

            Event event = guest.getEvent();

            // Check if already tracked
            if (linkClickRepository.existsByEventAndGuest(event, guest)) {
                System.out.println("ℹ️ Link click already tracked for this guest");
                System.out.println("========================================\n");

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Already tracked"
                ));
            }

            // Create new link click record
            RSVPLinkClick linkClick = new RSVPLinkClick();
            linkClick.setGuest(guest);
            linkClick.setEvent(event);
            linkClick.setClickedAt(LocalDateTime.now());

            // Get IP address
            String ipAddress = getClientIpAddress(request);
            linkClick.setIpAddress(ipAddress);

            // Get user agent
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 500); // Truncate if too long
            }
            linkClick.setUserAgent(userAgent);

            // Save
            linkClickRepository.save(linkClick);

            System.out.println("✅ Link click tracked successfully");
            System.out.println("   IP: " + ipAddress);
            System.out.println("   User Agent: " + (userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) + "..." : "N/A"));
            System.out.println("========================================\n");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Link click tracked"
            ));

        } catch (Exception e) {
            System.err.println("\n========== ERROR TRACKING LINK CLICK ==========");
            e.printStackTrace();
            System.err.println("===============================================\n");

            // Don't fail the request if tracking fails
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Tracking failed but continuing"
            ));
        }
    }

    /**
     * Helper method to get client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}