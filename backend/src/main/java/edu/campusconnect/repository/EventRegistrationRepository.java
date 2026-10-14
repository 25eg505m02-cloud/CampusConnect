package edu.campusconnect.repository;

import edu.campusconnect.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    long countByEventId(Long eventId);
    void deleteByEventIdAndUserId(Long eventId, Long userId);
}
