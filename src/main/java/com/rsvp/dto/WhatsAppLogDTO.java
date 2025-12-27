package com.rsvp.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class WhatsAppLogDTO {
    private String logId;
    private Long eventId;
    private Long guestId;
    private String phone;
    private String sentStatus;
    private LocalDateTime sentTimestamp;
    private String errorReason;
    private String messageVariant;
    private Integer retryCount;
}