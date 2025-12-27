package com.rsvp.dto;

import com.rsvp.entity.Guest;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GuestImportRequest {
    private List<GuestImportRow> guests;
    private String importedBy;
    private String fileName;
    private String fileType;
}

