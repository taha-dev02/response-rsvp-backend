package com.rsvp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "rsvps",
        uniqueConstraints = @UniqueConstraint(columnNames = {"submission_id"}),
        indexes = {
                @Index(name = "idx_rsvp_event", columnList = "event_id"),
                @Index(name = "idx_rsvp_guest", columnList = "guest_id"),
                @Index(name = "idx_rsvp_sync_status", columnList = "sync_status")
        }
)
@Data
public class RSVP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", unique = true, nullable = false, length = 100)
    private String submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @Column(name = "guest_name", length = 200)
    private String guestName;

    @Column(name = "guest_alias", length = 200)
    private String guestAlias;

    @Column(name = "group_name", length = 200)
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rsvp_status", nullable = false, length = 20)
    private RsvpStatus rsvpStatus;

    @Column(name = "guests_count")
    private Integer guestsCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source = Source.WEB_DIRECT;

    @Column(name = "submission_timestamp", nullable = false)
    private LocalDateTime submissionTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    private SyncStatus syncStatus = SyncStatus.SYNCED;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RsvpStatus {
        YES,
        NO
    }

    public enum Source {
        WEB_DIRECT,
        WHATSAPP,
        MANUAL
    }

    public enum SyncStatus {
        PENDING_SYNC,
        SYNCED,
        FAILED
    }
}