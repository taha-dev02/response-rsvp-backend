package com.rsvp.dto;

import com.rsvp.entity.Guest;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GuestImportRow {
    private String name;
    private String phone;
    private Integer maxInvitees;
    private String groupName;
    private List<String> aliases;
    private Integer rowNumber;
}