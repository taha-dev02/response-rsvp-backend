package com.rsvp.dto;

import lombok.Data;
import java.util.List;

@Data
public class WhatsAppSyncRequest {
    private List<WhatsAppLogDTO> logs;
}