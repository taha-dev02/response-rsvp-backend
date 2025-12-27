package com.rsvp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSummary {
    private String groupName;
    private Long totalMembers;
    private Long respondedCount;
    private Long attendingCount;
    private Long totalGuestsAttending;
}