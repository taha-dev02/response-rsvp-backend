package com.rsvp.service;

import com.rsvp.dto.*;
import com.rsvp.entity.*;
import com.rsvp.exception.ResourceNotFoundException;
import com.rsvp.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final WhatsAppLogRepository whatsAppLogRepository;
    private final EventRepository eventRepository;
    private final GuestRepository guestRepository;

    @Transactional
    public WhatsAppSyncResponse syncWhatsAppLogs(WhatsAppSyncRequest request) {
        int totalReceived = request.getLogs().size();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        for (WhatsAppLogDTO logDto : request.getLogs()) {
            try {
                // Check for duplicates
                if (logDto.getLogId() != null) {
                    Optional<WhatsAppLog> existing = whatsAppLogRepository.findByLogId(logDto.getLogId());
                    if (existing.isPresent()) {
                        log.info("Duplicate WhatsApp log ignored: {}", logDto.getLogId());
                        continue;
                    }
                }

                // Validate and fetch event
                Event event = eventRepository.findById(logDto.getEventId())
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + logDto.getEventId()));

                // Try to resolve guest
                Guest guest = null;
                if (logDto.getGuestId() != null) {
                    guest = guestRepository.findById(logDto.getGuestId()).orElse(null);
                }

                // Create WhatsApp log
                WhatsAppLog log = new WhatsAppLog();
                log.setLogId(logDto.getLogId() != null ? logDto.getLogId() : UUID.randomUUID().toString());
                log.setEvent(event);
                log.setGuest(guest);
                log.setPhone(logDto.getPhone());
                log.setSentStatus(WhatsAppLog.SentStatus.valueOf(logDto.getSentStatus()));
                log.setSentTimestamp(logDto.getSentTimestamp() != null ? logDto.getSentTimestamp() : LocalDateTime.now());
                log.setErrorReason(logDto.getErrorReason());
                log.setMessageVariant(logDto.getMessageVariant());
                log.setRetryCount(logDto.getRetryCount() != null ? logDto.getRetryCount() : 0);
                log.setSyncStatus(WhatsAppLog.SyncStatus.SYNCED);
                log.setSyncedAt(LocalDateTime.now());

                whatsAppLogRepository.save(log);
                successCount++;

            } catch (Exception e) {
                failCount++;
                errors.add("Log " + logDto.getLogId() + ": " + e.getMessage());
                log.error("Failed to sync WhatsApp log: {}", logDto.getLogId(), e);
            }
        }

        WhatsAppSyncResponse response = new WhatsAppSyncResponse();
        response.setTotalReceived(totalReceived);
        response.setSuccessfulSyncs(successCount);
        response.setFailedSyncs(failCount);
        response.setErrors(errors);

        return response;
    }

    @Transactional(readOnly = true)
    public List<WhatsAppLog> getLogsByEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        return whatsAppLogRepository.findByEvent(event);
    }

    @Transactional(readOnly = true)
    public List<WhatsAppLog> getLogsByGuest(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + guestId));
        return whatsAppLogRepository.findByGuest(guest);
    }

    @Transactional(readOnly = true)
    public Optional<WhatsAppLog> getLatestLogForGuest(Long eventId, Long guestId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + guestId));

        List<WhatsAppLog> logs = whatsAppLogRepository.findByEventAndGuestOrderByTimestampDesc(event, guest);
        return logs.isEmpty() ? Optional.empty() : Optional.of(logs.get(0));
    }
}