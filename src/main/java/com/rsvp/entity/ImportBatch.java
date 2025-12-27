package com.rsvp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_batches")
@Data
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", unique = true, nullable = false, length = 100)
    private String batchId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_type", length = 20)
    private String fileType;

    @Column(name = "total_rows")
    private Integer totalRows;

    @Column(name = "successful_imports")
    private Integer successfulImports;

    @Column(name = "failed_imports")
    private Integer failedImports;

    @Column(name = "imported_by", length = 100)
    private String importedBy;

    @Column(name = "column_mappings", columnDefinition = "TEXT")
    private String columnMappings; // JSON string

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportStatus status;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum ImportStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        PARTIAL_SUCCESS
    }
}