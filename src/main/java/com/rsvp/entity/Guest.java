

package com.rsvp.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guests", indexes = {
        @Index(name = "idx_guest_phone", columnList = "phone"),
        @Index(name = "idx_guest_group", columnList = "group_name"),
        @Index(name = "idx_guest_event", columnList = "event_id") // NEW INDEX
})
@Data
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guest_id", unique = true, nullable = false, length = 100)
    private String guestId;

    // NEW: Link to event
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "max_invitees")
    private Integer maxInvitees = 1;

    @Column(name = "group_name", length = 200)
    private String groupName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "guest_aliases", joinColumns = @JoinColumn(name = "guest_id"))
    @Column(name = "alias", length = 200)
    private List<String> aliases = new ArrayList<>();

    @Column(name = "imported_from_file")
    private Boolean importedFromFile = false;

    @Column(name = "import_batch_id", length = 100)
    private String importBatchId;

    @Column(name = "import_file_name", length = 500)
    private String importFileName;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "imported_by", length = 100)
    private String importedBy;

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

}

