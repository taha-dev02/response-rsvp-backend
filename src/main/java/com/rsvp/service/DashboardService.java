package com.rsvp.service;

import com.rsvp.dto.EventDashboardResponse;
import com.rsvp.dto.EventMetrics;
import com.rsvp.dto.GuestTableRow;
import com.rsvp.dto.GroupSummary;
import com.rsvp.entity.*;
import com.rsvp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final EventRepository eventRepository;
    private final GuestRepository guestRepository;
    private final RSVPRepository rsvpRepository;
    private final WhatsAppLogRepository whatsAppLogRepository;
    private final RSVPLinkClickRepository linkClickRepository;

    public DashboardService(EventRepository eventRepository,
                            GuestRepository guestRepository,
                            RSVPRepository rsvpRepository,
                            WhatsAppLogRepository whatsAppLogRepository,
                            RSVPLinkClickRepository linkClickRepository) {
        this.eventRepository = eventRepository;
        this.guestRepository = guestRepository;
        this.rsvpRepository = rsvpRepository;
        this.whatsAppLogRepository = whatsAppLogRepository;
        this.linkClickRepository = linkClickRepository;
    }

    @Transactional(readOnly = true)
    public EventDashboardResponse getEventDashboard(Long eventId) {
        // Fetch event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));

        // Get ALL data for this event
        List<Guest> allGuests = guestRepository.findByEvent(event);
        List<RSVP> allRsvps = rsvpRepository.findByEvent(event);
        List<WhatsAppLog> allLogs = whatsAppLogRepository.findByEvent(event);
        List<RSVPLinkClick> allClicks = linkClickRepository.findByEvent(event);

        // Create lookup maps
        Map<Long, RSVP> latestRsvpMap = createLatestRsvpMap(allRsvps);
        Map<Long, WhatsAppLog> latestLogMap = createLatestLogMap(allLogs);
        Map<Long, RSVPLinkClick> clickMap = createClickMap(allClicks);

        // Calculate metrics
        EventMetrics metrics = calculateMetrics(allGuests, latestRsvpMap, allLogs, allClicks);

        // Build guest table
        List<GuestTableRow> guestTable = allGuests.stream()
                .map(guest -> convertToTableRow(guest, latestRsvpMap, latestLogMap, clickMap))
                .collect(Collectors.toList());

        // Build group summaries
        List<GroupSummary> groupSummaries = buildGroupSummaries(allGuests, latestRsvpMap);

        // Build and return response
        return new EventDashboardResponse(
                event.getId(),
                event.getName(),
                event.getEventDateTime(),
                event.getVenue(),
                metrics,
                guestTable,
                groupSummaries
        );
    }

    private Map<Long, RSVP> createLatestRsvpMap(List<RSVP> allRsvps) {
        Map<Long, RSVP> map = new HashMap<>();
        for (RSVP rsvp : allRsvps) {
            if (rsvp.getGuest() != null) {
                Long guestId = rsvp.getGuest().getId();
                RSVP existing = map.get(guestId);
                if (existing == null ||
                        rsvp.getSubmissionTimestamp().isAfter(existing.getSubmissionTimestamp())) {
                    map.put(guestId, rsvp);
                }
            }
        }
        return map;
    }

    private Map<Long, WhatsAppLog> createLatestLogMap(List<WhatsAppLog> allLogs) {
        Map<Long, WhatsAppLog> map = new HashMap<>();
        for (WhatsAppLog log : allLogs) {
            if (log.getGuest() != null) {
                Long guestId = log.getGuest().getId();
                WhatsAppLog existing = map.get(guestId);
                if (existing == null ||
                        log.getSentTimestamp().isAfter(existing.getSentTimestamp())) {
                    map.put(guestId, log);
                }
            }
        }
        return map;
    }

    private Map<Long, RSVPLinkClick> createClickMap(List<RSVPLinkClick> allClicks) {
        Map<Long, RSVPLinkClick> map = new HashMap<>();
        for (RSVPLinkClick click : allClicks) {
            if (click.getGuest() != null) {
                map.put(click.getGuest().getId(), click);
            }
        }
        return map;
    }

    private EventMetrics calculateMetrics(List<Guest> guests,
                                          Map<Long, RSVP> rsvpMap,
                                          List<WhatsAppLog> logs,
                                          List<RSVPLinkClick> clicks) {

        // Total invites = sum of all maxInvitees (total capacity)
        long totalInvited = guests.stream()
                .mapToLong(g -> g.getMaxInvitees() != null ? g.getMaxInvitees() : 1)
                .sum();

        long confirmed = 0;      // Actual number of invitees attending
        long declined = 0;       // Explicit NO + unfilled capacity
        long pending = 0;        // Sum of maxInvitees for PENDING
        long totalAttending = 0;

        // Calculate RSVP metrics
        for (Guest guest : guests) {
            int maxInvitees = guest.getMaxInvitees() != null ? guest.getMaxInvitees() : 1;

            RSVP rsvp = rsvpMap.get(guest.getId());
            if (rsvp != null) {
                if (rsvp.getRsvpStatus() == RSVP.RsvpStatus.YES) {
                    int attending = rsvp.getGuestsCount();
                    confirmed += attending;
                    totalAttending += attending;

                    // Add unfilled capacity to declined
                    int unfilled = maxInvitees - attending;
                    if (unfilled > 0) {
                        declined += unfilled;
                    }
                } else if (rsvp.getRsvpStatus() == RSVP.RsvpStatus.NO) {
                    declined += maxInvitees;   // Explicit NO
                }
            } else {
                pending += maxInvitees;        // No response yet
            }
        }

        // Calculate WhatsApp metrics
        long whatsappSent = logs.stream()
                .filter(log -> log.getSentStatus() == WhatsAppLog.SentStatus.SENT)
                .count();

        long whatsappFailed = logs.stream()
                .filter(log -> log.getSentStatus() == WhatsAppLog.SentStatus.FAILED)
                .count();

        // Calculate link click metrics
        long linkClicks = clicks.size();
        double clickRate = whatsappSent > 0 ? (linkClicks * 100.0 / whatsappSent) : 0.0;
        long clickedButNotResponded = linkClicks - (confirmed + declined);
        if (clickedButNotResponded < 0) clickedButNotResponded = 0;

        // Calculate response rate
        double responseRate = totalInvited > 0 ?
                ((confirmed + declined) * 100.0 / totalInvited) : 0.0;

        // Return metrics with proper rounding
        return new EventMetrics(
                totalInvited,
                confirmed,
                declined,
                pending,
                totalAttending,
                Math.round(responseRate * 100.0) / 100.0,
                whatsappSent,
                whatsappFailed,
                linkClicks,
                Math.round(clickRate * 100.0) / 100.0,
                clickedButNotResponded
        );
    }

    private GuestTableRow convertToTableRow(Guest guest,
                                            Map<Long, RSVP> rsvpMap,
                                            Map<Long, WhatsAppLog> logMap,
                                            Map<Long, RSVPLinkClick> clickMap) {

        // RSVP info
        RSVP rsvp = rsvpMap.get(guest.getId());
        String rsvpStatus = rsvp != null ? rsvp.getRsvpStatus().name() : "PENDING";
        Integer guestsAttending = (rsvp != null && rsvp.getRsvpStatus() == RSVP.RsvpStatus.YES) ?
                rsvp.getGuestsCount() : 0;

        // WhatsApp info
        WhatsAppLog log = logMap.get(guest.getId());
        String whatsappStatus = log != null ? log.getSentStatus().name() : "NOT_SENT";

        // Link click info
        RSVPLinkClick click = clickMap.get(guest.getId());
        Boolean linkClicked = click != null;
        LocalDateTime linkClickedAt = click != null ? click.getClickedAt() : null;

        // Last activity
        LocalDateTime lastActivity = null;
        if (rsvp != null && log != null) {
            lastActivity = rsvp.getSubmissionTimestamp().isAfter(log.getSentTimestamp()) ?
                    rsvp.getSubmissionTimestamp() : log.getSentTimestamp();
        } else if (rsvp != null) {
            lastActivity = rsvp.getSubmissionTimestamp();
        } else if (log != null) {
            lastActivity = log.getSentTimestamp();
        }

        return new GuestTableRow(
                guest.getId(),
                guest.getName(),
                guest.getPhone(),
                guest.getGroupName(),
                guest.getMaxInvitees(),
                rsvpStatus,
                guestsAttending,
                whatsappStatus,
                lastActivity,
                guest.getAliases(),
                linkClicked,
                linkClickedAt
        );
    }

    private List<GroupSummary> buildGroupSummaries(List<Guest> allGuests,
                                                   Map<Long, RSVP> rsvpMap) {
        // Group guests by group name
        Map<String, List<Guest>> groupedByName = allGuests.stream()
                .collect(Collectors.groupingBy(g ->
                        g.getGroupName() != null ? g.getGroupName() : "Ungrouped"));

        return groupedByName.entrySet().stream()
                .map(entry -> {
                    String groupName = entry.getKey();
                    List<Guest> groupGuests = entry.getValue();

                    long totalMembers = groupGuests.size();
                    long respondedCount = 0;
                    long attendingCount = 0;
                    long totalGuestsAttending = 0;

                    for (Guest guest : groupGuests) {
                        RSVP rsvp = rsvpMap.get(guest.getId());
                        if (rsvp != null) {
                            respondedCount++;
                            if (rsvp.getRsvpStatus() == RSVP.RsvpStatus.YES) {
                                attendingCount++;
                                totalGuestsAttending += rsvp.getGuestsCount();
                            }
                        }
                    }

                    return new GroupSummary(
                            groupName,
                            totalMembers,
                            respondedCount,
                            attendingCount,
                            totalGuestsAttending
                    );
                })
                .collect(Collectors.toList());
    }
}