package edu.campusconnect.repository;

import edu.campusconnect.entity.GroupMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    Optional<GroupMembership> findByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupMembership> findByUserId(Long userId);
    long countByGroupId(Long groupId);
    void deleteByGroupIdAndUserId(Long groupId, Long userId);
}
