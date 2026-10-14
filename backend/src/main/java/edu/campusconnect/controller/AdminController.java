package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.AdminDashboardResponse;
import edu.campusconnect.dto.ApiDtos.ReportResponse;
import edu.campusconnect.dto.ApiDtos.ReportStatusRequest;
import edu.campusconnect.dto.ApiDtos.RoleRequest;
import edu.campusconnect.dto.ApiDtos.UserResponse;
import edu.campusconnect.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return admin.dashboard();
    }

    @GetMapping("/users")
    public Page<UserResponse> users(@PageableDefault(size = 50) Pageable pageable) {
        return admin.listUsers(pageable);
    }

    @PatchMapping("/users/{id}/role")
    public UserResponse changeRole(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {
        return admin.changeRole(currentUser.getUsername(), id, request.role());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableUser(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        admin.disableUser(currentUser.getUsername(), id);
    }

    @GetMapping("/reports")
    public Page<ReportResponse> reports(@PageableDefault(size = 50) Pageable pageable) {
        return admin.reports(pageable);
    }

    @PatchMapping("/reports/{id}/status")
    public ReportResponse updateReportStatus(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusRequest request) {
        return admin.updateReportStatus(currentUser.getUsername(), id, request.status());
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeReportedPost(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        admin.removeReportedPost(currentUser.getUsername(), id);
    }
}
