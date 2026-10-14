package edu.campusconnect.repository;

import edu.campusconnect.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);
    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);
}
