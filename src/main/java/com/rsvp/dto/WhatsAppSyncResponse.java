
package com.rsvp.dto;

import lombok.Data;
import java.util.List;

@Data
public class WhatsAppSyncResponse {
    private Integer totalReceived;
    private Integer successfulSyncs;
    private Integer failedSyncs;
    private List<String> errors;
}