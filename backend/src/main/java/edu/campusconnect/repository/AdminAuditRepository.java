package edu.campusconnect.repository;

import edu.campusconnect.entity.AdminAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditRepository extends JpaRepository<AdminAudit, Long> {
}
