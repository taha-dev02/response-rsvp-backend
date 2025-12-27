package com.rsvp.repository;

import com.rsvp.entity.Event;
import com.rsvp.entity.Guest;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    // ✅ ORIGINAL METHODS - Keep for backward compatibility
    Optional<Guest> findByGuestId(String guestId);

    @Modifying
    @Transactional
    void deleteByEvent_Id(Long eventId);

    List<Guest> findByGroupName(String groupName);
    List<Guest> findByImportBatchId(String importBatchId);
    List<Guest> findByEvent(Event event);

    @Query("SELECT g FROM Guest g WHERE :alias MEMBER OF g.aliases")
    List<Guest> findByAlias(@Param("alias") String alias);

    @Query("SELECT g FROM Guest g WHERE LOWER(g.name) = LOWER(:name) AND g.event = :event")
    Optional<Guest> findByNameAndEvent(@Param("name") String name, @Param("event") Event event);

    @Query("SELECT DISTINCT g.groupName FROM Guest g WHERE g.groupName IS NOT NULL AND g.event = :event ORDER BY g.groupName")
    List<String> findAllGroupNamesByEvent(@Param("event") Event event);

    @Query("SELECT DISTINCT g.groupName FROM Guest g WHERE g.groupName IS NOT NULL ORDER BY g.groupName")
    List<String> findAllGroupNames();

    @Query("SELECT g FROM Guest g WHERE LOWER(g.name) = LOWER(:name)")
    Optional<Guest> findByNameIgnoreCase(@Param("name") String name);

    Optional<Guest> findByEventIdAndName(Long eventId, String fullName);

    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM Guest g WHERE g.event.id = :eventId AND LOWER(g.name) = LOWER(:name)")
    boolean existsByEventIdAndNameIgnoreCase(Long eventId, String name);

    // ========================================================================
    // 🚀 OPTIMIZED METHODS - Use these to eliminate N+1 queries
    // ========================================================================

    /**
     * 🔥 FIX #1: Load guests with event in single query
     * Use this when you need to access guest.getEvent()
     *
     * Problem: findByEvent() loads guests, then lazily loads event for each (N+1)
     * Solution: JOIN FETCH loads event in same query
     */
    @Query("SELECT g FROM Guest g " +
            "LEFT JOIN FETCH g.event " +
            "WHERE g.event = :event " +
            "ORDER BY g.name")
    List<Guest> findByEventWithEvent(@Param("event") Event event);

    /**
     * 🔥 FIX #2: Load guests by event ID with event data
     * More efficient when you only have eventId
     */
    @Query("SELECT g FROM Guest g " +
            "LEFT JOIN FETCH g.event e " +
            "WHERE e.id = :eventId " +
            "ORDER BY g.name")
    List<Guest> findByEventIdWithEvent(@Param("eventId") Long eventId);

    /**
     * 🔥 FIX #3: Load single guest with event
     * Use in RSVPService when resolving guests
     */
    @Query("SELECT g FROM Guest g " +
            "LEFT JOIN FETCH g.event " +
            "WHERE g.guestId = :guestId")
    Optional<Guest> findByGuestIdWithEvent(@Param("guestId") String guestId);

    /**
     * 🔥 FIX #4: Load guests by alias with event
     * Optimizes RSVPService.resolveGuest()
     */
    @Query("SELECT g FROM Guest g " +
            "LEFT JOIN FETCH g.event " +
            "WHERE :alias MEMBER OF g.aliases")
    List<Guest> findByAliasWithEvent(@Param("alias") String alias);

    /**
     * 🔥 FIX #5: Load guests by group with event
     * Use for group RSVP pages
     */
    @Query("SELECT g FROM Guest g " +
            "LEFT JOIN FETCH g.event " +
            "WHERE g.groupName = :groupName " +
            "ORDER BY g.name")
    List<Guest> findByGroupNameWithEvent(@Param("groupName") String groupName);

    /**
     * 🔥 FIX #6: Batch count queries for performance
     * Use these instead of loading full entities when you just need counts
     */
    @Query("SELECT COUNT(g) FROM Guest g WHERE g.event.id = :eventId")
    Long countByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(g) FROM Guest g WHERE g.event.id = :eventId AND g.groupName = :groupName")
    Long countByEventIdAndGroupName(@Param("eventId") Long eventId, @Param("groupName") String groupName);

    /**
     * 🔥 FIX #7: Optimized bulk delete
     * More efficient than deleteByEvent_Id for large datasets
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Guest g WHERE g.event.id = :eventId")
    void bulkDeleteByEventId(@Param("eventId") Long eventId);
}