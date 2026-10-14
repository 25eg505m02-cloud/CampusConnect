package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.ReportRequest;
import edu.campusconnect.dto.ApiDtos.ReportResponse;
import edu.campusconnect.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(
            @AuthenticationPrincipal UserDetails currentUser, @Valid @RequestBody ReportRequest request) {
        return reports.create(currentUser.getUsername(), request);
    }
}
