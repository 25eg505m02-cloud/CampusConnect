package edu.campusconnect.repository;

import edu.campusconnect.entity.CampusGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusGroupRepository extends JpaRepository<CampusGroup, Long> {
    Page<CampusGroup> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByCreatorId(Long creatorId);
}
