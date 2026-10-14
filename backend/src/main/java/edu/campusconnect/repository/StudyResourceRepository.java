package edu.campusconnect.repository;

import edu.campusconnect.entity.StudyResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyResourceRepository extends JpaRepository<StudyResource, Long> {
    Page<StudyResource> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<StudyResource> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByCreatedAtDesc(
            String title, String description, Pageable pageable);
    long countByUploaderId(Long uploaderId);
}
