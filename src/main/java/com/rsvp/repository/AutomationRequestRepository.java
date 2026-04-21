package com.rsvp.repository;

import com.rsvp.entity.AutomationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AutomationRequestRepository extends JpaRepository<AutomationRequest, Long> {

    List<AutomationRequest> findByEventId(Long eventId);

    List<AutomationRequest> findByStatus(AutomationRequest.AutomationStatus status);
}
