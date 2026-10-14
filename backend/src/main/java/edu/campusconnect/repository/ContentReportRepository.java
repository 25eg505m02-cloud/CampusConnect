package edu.campusconnect.repository;

import edu.campusconnect.entity.ContentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {
    Page<ContentReport> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
