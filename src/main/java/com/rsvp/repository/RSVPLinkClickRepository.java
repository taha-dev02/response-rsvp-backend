package com.rsvp.repository;

import com.rsvp.entity.Event;
import com.rsvp.entity.Guest;
import com.rsvp.entity.RSVPLinkClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RSVPLinkClickRepository extends JpaRepository<RSVPLinkClick, Long> {

    // Find all clicks for a specific event
    List<RSVPLinkClick> findByEvent(Event event);

    // Find all clicks by a specific guest
    List<RSVPLinkClick> findByGuest(Guest guest);

    // Find click by event and guest (to check if guest clicked)
    Optional<RSVPLinkClick> findByEventAndGuest(Event event, Guest guest);

    // Count total unique guests who clicked for an event
    @Query("SELECT COUNT(DISTINCT c.guest.id) FROM RSVPLinkClick c WHERE c.event = :event")
    long countDistinctByEvent(@Param("event") Event event);

    // Count total clicks for an event (including multiple clicks by same guest)
    long countByEvent(Event event);

    // Check if a guest has clicked their link
    boolean existsByEventAndGuest(Event event, Guest guest);

    // Find the first/earliest click by a guest for an event
    Optional<RSVPLinkClick> findFirstByEventAndGuestOrderByClickedAtAsc(Event event, Guest guest);

    // Get all guests who clicked but haven't submitted RSVP
    @Query("SELECT c FROM RSVPLinkClick c WHERE c.event = :event " +
            "AND NOT EXISTS (SELECT r FROM RSVP r WHERE r.event = :event AND r.guest = c.guest)")
    List<RSVPLinkClick> findClickedButNotResponded(@Param("event") Event event);
}
