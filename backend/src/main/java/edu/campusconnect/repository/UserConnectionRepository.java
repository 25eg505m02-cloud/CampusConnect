package edu.campusconnect.repository;

import edu.campusconnect.entity.ConnectionStatus;
import edu.campusconnect.entity.UserConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserConnectionRepository extends JpaRepository<UserConnection, Long> {
    Optional<UserConnection> findByRequesterIdAndRecipientId(Long requesterId, Long recipientId);
    List<UserConnection> findByRequesterIdAndStatusOrRecipientIdAndStatus(
            Long requesterId, ConnectionStatus requesterStatus, Long recipientId, ConnectionStatus recipientStatus);
    List<UserConnection> findByRecipientIdAndStatus(Long recipientId, ConnectionStatus status);
}
