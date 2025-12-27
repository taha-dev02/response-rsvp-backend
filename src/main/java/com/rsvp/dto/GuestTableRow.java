package com.rsvp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestTableRow {
    private Long guestId;
    private String name;
    private String phone;
    private String groupName;
    private Integer maxInvitees;
    private String rsvpStatus;
    private Integer guestsAttending;
    private String whatsappStatus;
    private LocalDateTime lastActivity;
    private List<String> aliases;
    private Boolean linkClicked;
    private LocalDateTime linkClickedAt;
}
