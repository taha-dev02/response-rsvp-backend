package com.rsvp.repository;

import com.rsvp.entity.Event;
import com.rsvp.entity.Guest;
import com.rsvp.entity.RSVP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RSVPRepository extends JpaRepository<RSVP, Long> {

    // ✅ ORIGINAL METHODS - Keep for backward compatibility
    Optional<RSVP> findBySubmissionId(String submissionId);
    List<RSVP> findByEvent(Event event);
    List<RSVP> findByEventAndGuest(Event event, Guest guest);
    List<RSVP> findByEventAndRsvpStatus(Event event, RSVP.RsvpStatus status);
    List<RSVP> findBySyncStatus(RSVP.SyncStatus syncStatus);

    @Query("SELECT COUNT(r) FROM RSVP r WHERE r.event = :event AND r.rsvpStatus = :status")
    Long countByEventAndStatus(@Param("event") Event event, @Param("status") RSVP.RsvpStatus status);

    @Query("SELECT SUM(r.guestsCount) FROM RSVP r WHERE r.event = :event AND r.rsvpStatus = 'YES'")
    Long sumGuestsCountByEvent(@Param("event") Event event);

    @Query("SELECT r FROM RSVP r WHERE r.event = :event AND r.guest = :guest ORDER BY r.submissionTimestamp DESC")
    List<RSVP> findLatestByEventAndGuest(@Param("event") Event event, @Param("guest") Guest guest);

    // ========================================================================
    // 🚀 OPTIMIZED METHODS - Use these to eliminate N+1 queries
    // ========================================================================

    /**
     * 🔥 FIX #1: Load RSVPs with event and guest in single query
     * Use this in RSVPService.getRSVPsByEvent()
     *
     * Problem: findByEvent() loads RSVPs, then lazily loads event & guest (2N+1 queries)
     * Solution: JOIN FETCH loads all data in 1 query
     */
    @Query("SELECT DISTINCT r FROM RSVP r " +
            "LEFT JOIN FETCH r.event e " +
            "LEFT JOIN FETCH r.guest g " +
            "WHERE r.event = :event " +
            "ORDER BY r.submissionTimestamp DESC")
    List<RSVP> findByEventWithRelations(@Param("event") Event event);

    /**
     * 🔥 FIX #2: Load RSVPs by event ID with all relations
     * More efficient when you only have eventId
     */
    @Query("SELECT DISTINCT r FROM RSVP r " +
            "LEFT JOIN FETCH r.event e " +
            "LEFT JOIN FETCH r.guest g " +
            "WHERE e.id = :eventId " +
            "ORDER BY r.submissionTimestamp DESC")
    List<RSVP> findByEventIdWithRelations(@Param("eventId") Long eventId);

    /**
     * 🔥 FIX #3: Load RSVPs by status with relations
     * Use in RSVPService.getRSVPsByEventAndStatus()
     */
    @Query("SELECT DISTINCT r FROM RSVP r " +
            "LEFT JOIN FETCH r.event e " +
            "LEFT JOIN FETCH r.guest g " +
            "WHERE r.event = :event AND r.rsvpStatus = :status " +
            "ORDER BY r.submissionTimestamp DESC")
    List<RSVP> findByEventAndRsvpStatusWithRelations(
            @Param("event") Event event,
            @Param("status") RSVP.RsvpStatus status
    );

    /**
     * 🔥 FIX #4: Load latest RSVP with relations
     * Optimizes RSVPService.getLatestRSVPForGuest()
     */
    @Query("SELECT r FROM RSVP r " +
            "LEFT JOIN FETCH r.event e " +
            "LEFT JOIN FETCH r.guest g " +
            "WHERE r.event = :event AND r.guest = :guest " +
            "ORDER BY r.submissionTimestamp DESC")
    List<RSVP> findLatestByEventAndGuestWithRelations(
            @Param("event") Event event,
            @Param("guest") Guest guest
    );

    /**
     * 🔥 FIX #5: Check if submission exists (optimized)
     * Use this instead of findBySubmissionId when you only need to check existence
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
            "FROM RSVP r WHERE r.submissionId = :submissionId")
    boolean existsBySubmissionId(@Param("submissionId") String submissionId);

    /**
     * 🔥 FIX #6: Batch count queries for dashboard
     * These are optimized and don't load full entities
     */
    @Query("SELECT r.rsvpStatus, COUNT(r) FROM RSVP r " +
            "WHERE r.event.id = :eventId " +
            "GROUP BY r.rsvpStatus")
    List<Object[]> countByStatusGrouped(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(r.guestsCount), 0) FROM RSVP r " +
            "WHERE r.event.id = :eventId AND r.rsvpStatus = :status")
    Long sumGuestsCountByEventIdAndStatus(
            @Param("eventId") Long eventId,
            @Param("status") RSVP.RsvpStatus status
    );

    /**
     * 🔥 FIX #7: Get RSVPs pending sync (for background jobs)
     */
    @Query("SELECT r FROM RSVP r " +
            "LEFT JOIN FETCH r.event " +
            "WHERE r.syncStatus = :syncStatus " +
            "ORDER BY r.submissionTimestamp DESC")
    List<RSVP> findBySyncStatusWithEvent(@Param("syncStatus") RSVP.SyncStatus syncStatus);
}