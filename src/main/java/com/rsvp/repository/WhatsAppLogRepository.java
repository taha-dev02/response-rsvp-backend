package com.rsvp.repository;

import com.rsvp.entity.Event;
import com.rsvp.entity.Guest;
import com.rsvp.entity.RSVP;
import com.rsvp.entity.WhatsAppLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface WhatsAppLogRepository extends JpaRepository<WhatsAppLog, Long> {
    List<WhatsAppLog> findByEvent(Event event);
    List<WhatsAppLog> findByGuest(Guest guest);
    List<WhatsAppLog> findBySyncStatus(WhatsAppLog.SyncStatus syncStatus);
    Optional<WhatsAppLog> findByLogId(String logId);

    @Query("SELECT w FROM WhatsAppLog w WHERE w.event = :event AND w.guest = :guest ORDER BY w.sentTimestamp DESC")
    List<WhatsAppLog> findByEventAndGuestOrderByTimestampDesc(@Param("event") Event event, @Param("guest") Guest guest);
}