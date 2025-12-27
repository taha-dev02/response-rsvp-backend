package com.rsvp.repository;

import com.rsvp.entity.Event;
import com.rsvp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // ✅ Basic queries without fetch joins
    List<Event> findByStatusOrderByEventDateTimeDesc(Event.EventStatus status);
    List<Event> findAllByOrderByEventDateTimeDesc();
    Optional<Event> findById(Long eventId);
    List<Event> findByCreatedByOrderByEventDateTimeDesc(String createdBy);
    List<Event> findByOwner(User owner);

    // ✅ Query with owner only (no guests - avoids Cartesian product)
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.owner " +
            "ORDER BY e.eventDateTime DESC")
    List<Event> findAllWithOwner();

    // ✅ Query with owner only for specific owner
    @Query("SELECT DISTINCT e FROM Event e " +
            "LEFT JOIN FETCH e.owner o " +
            "WHERE o = :owner " +
            "ORDER BY e.eventDateTime DESC")
    List<Event> findByOwnerWithOwner(@Param("owner") User owner);

    // ✅ Single event with owner
    @Query("SELECT e FROM Event e " +
            "LEFT JOIN FETCH e.owner " +
            "WHERE e.id = :id")
    Optional<Event> findByIdWithOwner(@Param("id") Long id);

    // ⚠️ REMOVED: findByIdWithOwnerAndGuests - causes Cartesian product
    // If you need event with guests, fetch event first, then fetch guests separately:
    // Event event = eventRepository.findByIdWithOwner(id);
    // List<Guest> guests = guestRepository.findByEvent(event);

    // ⚠️ REMOVED: findAllWithOwnerAndGuests - causes Cartesian product
    // Use findAllWithOwner() instead and fetch guests separately when needed

    // ✅ Count queries
    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status")
    Long countByStatus(@Param("status") Event.EventStatus status);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.owner = :owner")
    Long countByOwner(@Param("owner") User owner);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.owner = :owner AND e.status = :status")
    Long countByOwnerAndStatus(@Param("owner") User owner, @Param("status") Event.EventStatus status);
}