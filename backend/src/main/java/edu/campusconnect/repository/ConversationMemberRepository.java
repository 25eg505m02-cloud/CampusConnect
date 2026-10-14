package edu.campusconnect.repository;

import edu.campusconnect.entity.ConversationMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);
    List<ConversationMember> findByConversationId(Long conversationId);
    Page<ConversationMember> findByUserId(Long userId, Pageable pageable);
    long countByConversationId(Long conversationId);
}
