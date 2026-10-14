package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.ReportRequest;
import edu.campusconnect.dto.ApiDtos.ReportResponse;
import edu.campusconnect.entity.ContentReport;
import edu.campusconnect.entity.ReportStatus;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.ContentReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final ContentReportRepository reports;
    private final EntityLookup lookup;

    public ReportService(ContentReportRepository reports, EntityLookup lookup) {
        this.reports = reports;
        this.lookup = lookup;
    }

    @Transactional
    public ReportResponse create(String email, ReportRequest request) {
        User reporter = lookup.userByEmail(email);
        String type = request.targetType();
        switch (type) {
            case "POST" -> lookup.post(request.targetId());
            case "COMMENT" -> lookup.comment(request.targetId());
            case "USER" -> lookup.userById(request.targetId());
            case "GROUP" -> lookup.group(request.targetId());
            default -> throw ApiException.badRequest("Unsupported report target type.");
        }
        ContentReport report = new ContentReport();
        report.setTargetType(type);
        report.setTargetId(request.targetId());
        report.setReason(request.reason().trim());
        report.setStatus(ReportStatus.OPEN);
        report.setReporter(reporter);
        return toResponse(reports.save(report));
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> list(Pageable pageable) {
        return reports.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional
    public ReportResponse setStatus(Long id, ReportStatus status) {
        ContentReport report = reports.findById(id)
                .orElseThrow(() -> ApiException.notFound("Report not found."));
        report.setStatus(status);
        return toResponse(report);
    }

    private ReportResponse toResponse(ContentReport report) {
        return new ReportResponse(report.getId(), report.getTargetType(), report.getTargetId(),
                report.getReason(), report.getStatus(), report.getReporter().getId(),
                report.getReporter().getFullName(), report.getCreatedAt());
    }
}
