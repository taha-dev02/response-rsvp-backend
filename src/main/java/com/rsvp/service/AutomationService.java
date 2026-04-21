package com.rsvp.service;

import com.rsvp.entity.AutomationRequest;
import com.rsvp.entity.AutomationRequest.AutomationStatus;
import com.rsvp.exception.ResourceNotFoundException;
import com.rsvp.repository.AutomationRequestRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutomationService {

    private final AutomationRequestRepository automationRequestRepository;
    private final JavaMailSender mailSender;

    @Value("${frontend.url:https://response-rsvp.vercel.app}")
    private String frontendUrl;

    @Value("${admin.email:tahaspringboot@gmail.com}")
    private String adminEmail;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Creates a new automation request and sends an approval email to the admin.
     */
    @Transactional
    public AutomationRequest createRequest(Long eventId, String eventName) {
        AutomationRequest request = new AutomationRequest();
        request.setEventId(eventId);
        request.setEventName(eventName);
        request.setStatus(AutomationStatus.PENDING_APPROVAL);

        AutomationRequest saved = automationRequestRepository.save(request);
        log.info("Created automation request {} for event '{}' (eventId={})", saved.getId(), eventName, eventId);

        // Send approval email to admin
        sendApprovalEmail(saved);

        return saved;
    }

    /**
     * Approves the automation request (sets status to APPROVED).
     */
    @Transactional
    public AutomationRequest approveRequest(Long requestId) {
        AutomationRequest request = automationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation request not found: " + requestId));

        if (request.getStatus() != AutomationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Request is already " + request.getStatus() + " and cannot be approved.");
        }

        request.setStatus(AutomationStatus.APPROVED);
        request.setApprovedAt(LocalDateTime.now());
        AutomationRequest updated = automationRequestRepository.save(request);

        log.info("Approved automation request {} for event '{}'", requestId, request.getEventName());
        return updated;
    }

    /**
     * Rejects the automation request (sets status to REJECTED).
     */
    @Transactional
    public AutomationRequest rejectRequest(Long requestId) {
        AutomationRequest request = automationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation request not found: " + requestId));

        if (request.getStatus() != AutomationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Request is already " + request.getStatus() + " and cannot be rejected.");
        }

        request.setStatus(AutomationStatus.REJECTED);
        AutomationRequest updated = automationRequestRepository.save(request);

        log.info("Rejected automation request {} for event '{}'", requestId, request.getEventName());
        return updated;
    }

    /**
     * Returns the current status of an automation request.
     */
    @Transactional(readOnly = true)
    public AutomationRequest getStatus(Long requestId) {
        return automationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Automation request not found: " + requestId));
    }

    /**
     * Sends a styled HTML approval email to the admin with approve/reject link.
     */
    public void sendApprovalEmail(AutomationRequest request) {
        try {
            String approvalLink = frontendUrl + "/approve/" + request.getId();

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0; padding:0; background-color:#f4f4f7; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f4f4f7; padding:40px 20px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="background-color:#ffffff; border-radius:12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); overflow:hidden;">
                                    <!-- Header -->
                                    <tr>
                                        <td style="background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%); padding:32px 40px; text-align:center;">
                                            <h1 style="color:#ffffff; margin:0; font-size:24px; font-weight:700;">🤖 WhatsApp Automation Request</h1>
                                        </td>
                                    </tr>
                                    <!-- Body -->
                                    <tr>
                                        <td style="padding:40px;">
                                            <p style="color:#374151; font-size:16px; line-height:1.6; margin:0 0 20px;">
                                                A new WhatsApp automation has been requested and is awaiting your approval.
                                            </p>
                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#f9fafb; border-radius:8px; border:1px solid #e5e7eb; margin-bottom:24px;">
                                                <tr>
                                                    <td style="padding:20px;">
                                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                                            <tr>
                                                                <td style="padding:4px 0; color:#6b7280; font-size:14px; width:120px;">Request ID:</td>
                                                                <td style="padding:4px 0; color:#111827; font-size:14px; font-weight:600;">#%d</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:4px 0; color:#6b7280; font-size:14px;">Event Name:</td>
                                                                <td style="padding:4px 0; color:#111827; font-size:14px; font-weight:600;">%s</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:4px 0; color:#6b7280; font-size:14px;">Event ID:</td>
                                                                <td style="padding:4px 0; color:#111827; font-size:14px; font-weight:600;">%d</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:4px 0; color:#6b7280; font-size:14px;">Status:</td>
                                                                <td style="padding:4px 0;">
                                                                    <span style="display:inline-block; padding:4px 12px; background-color:#fef3c7; color:#92400e; border-radius:9999px; font-size:12px; font-weight:600;">⏳ PENDING APPROVAL</span>
                                                                </td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="color:#374151; font-size:16px; line-height:1.6; margin:0 0 24px;">
                                                Click the button below to review and approve or reject this automation request:
                                            </p>
                                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" style="display:inline-block; padding:14px 40px; background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%); color:#ffffff; text-decoration:none; border-radius:8px; font-size:16px; font-weight:600; box-shadow: 0 4px 6px rgba(99,102,241,0.3);">
                                                            Review &amp; Approve
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="color:#9ca3af; font-size:13px; margin:24px 0 0; text-align:center;">
                                                Or copy this link: <a href="%s" style="color:#6366f1;">%s</a>
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- Footer -->
                                    <tr>
                                        <td style="background-color:#f9fafb; padding:20px 40px; border-top:1px solid #e5e7eb; text-align:center;">
                                            <p style="color:#9ca3af; font-size:12px; margin:0;">
                                                Response RSVP — WhatsApp Automation System
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                    request.getId(),
                    request.getEventName(),
                    request.getEventId(),
                    approvalLink,
                    approvalLink,
                    approvalLink
            );

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("🔔 WhatsApp Automation Approval Required — " + request.getEventName());
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Approval email sent to {} for request #{}", adminEmail, request.getId());

        } catch (MessagingException e) {
            log.error("Failed to send approval email for request #{}: {}", request.getId(), e.getMessage(), e);
            // Don't throw — the request is still created, email failure shouldn't block the flow
        }
    }
}
