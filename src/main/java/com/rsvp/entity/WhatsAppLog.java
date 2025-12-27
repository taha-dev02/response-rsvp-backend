package com.rsvp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_logs", indexes = {
        @Index(name = "idx_wa_event", columnList = "event_id"),
        @Index(name = "idx_wa_guest", columnList = "guest_id"),
        @Index(name = "idx_wa_sync", columnList = "sync_status")
})
@Data
public class WhatsAppLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "log_id", unique = true, length = 100)
    private String logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "sent_status", nullable = false, length = 20)
    private SentStatus sentStatus;

    @Column(name = "sent_timestamp", nullable = false)
    private LocalDateTime sentTimestamp;

    @Column(name = "error_reason", columnDefinition = "TEXT")
    private String errorReason;

    @Column(name = "message_variant", length = 100)
    private String messageVariant;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 20)
    private SyncStatus syncStatus = SyncStatus.PENDING_SYNC;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SentStatus {
        SENT,
        FAILED,
        PENDING
    }

    public enum SyncStatus {
        PENDING_SYNC,
        SYNCED,
        FAILED
    }
}