package com.rsvp.dto;

import com.rsvp.entity.Guest;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GuestImportResponse {
    private String batchId;
    private Integer totalRows;
    private Integer successfulImports;
    private Integer failedImports;
    private List<String> errors;
    private String status;
}