package com.rsvp.repository;

import com.rsvp.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find by username
    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);

    // Find by action
    List<AuditLog> findByActionOrderByTimestampDesc(String action);

    // Find by entity
    List<AuditLog> findByEntityOrderByTimestampDesc(String entity);

    // Find by date range
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate);

    // Find by username and action
    List<AuditLog> findByUsernameAndActionOrderByTimestampDesc(String username, String action);

    // Find by entity and entity ID
    List<AuditLog> findByEntityAndEntityIdOrderByTimestampDesc(String entity, Long entityId);

    // Find recent logs
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp > :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentLogs(@Param("since") LocalDateTime since);

    // Count by action
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action")
    Long countByAction(@Param("action") String action);

    // Find suspicious activity (multiple failed attempts)
    @Query("SELECT a FROM AuditLog a WHERE a.action = 'LOGIN_FAILED' AND a.username = :username " +
            "AND a.timestamp > :since ORDER BY a.timestamp DESC")
    List<AuditLog> findFailedLoginAttempts(@Param("username") String username,
                                           @Param("since") LocalDateTime since);

    // Delete old logs (for cleanup jobs)
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :cutoffDate")
    void deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
}