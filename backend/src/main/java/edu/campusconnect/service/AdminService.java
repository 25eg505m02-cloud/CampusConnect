package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.AdminDashboardResponse;
import edu.campusconnect.dto.ApiDtos.ReportResponse;
import edu.campusconnect.dto.ApiDtos.UserResponse;
import edu.campusconnect.entity.AdminAudit;
import edu.campusconnect.entity.ReportStatus;
import edu.campusconnect.entity.Role;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository users;
    private final PostRepository posts;
    private final CampusGroupRepository groups;
    private final EventRepository events;
    private final AdminAuditRepository audit;
    private final EntityLookup lookup;
    private final UserService userService;
    private final ReportService reports;
    private final PostService postsService;

    public AdminService(UserRepository users, PostRepository posts, CampusGroupRepository groups,
                        EventRepository events, AdminAuditRepository audit, EntityLookup lookup,
                        UserService userService, ReportService reports, PostService postsService) {
        this.users = users;
        this.posts = posts;
        this.groups = groups;
        this.events = events;
        this.audit = audit;
        this.lookup = lookup;
        this.userService = userService;
        this.reports = reports;
        this.postsService = postsService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return new AdminDashboardResponse(users.countByEnabledTrue(),
                users.countByRoleAndEnabledTrue(Role.STUDENT),
                users.countByRoleAndEnabledTrue(Role.FACULTY),
                users.countByRoleAndEnabledTrue(Role.ADMIN),
                posts.count(), groups.count(), events.count());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable) {
        return users.findAllByEnabledTrue(pageable).map(userService::toResponse);
    }

    @Transactional
    public UserResponse changeRole(String adminEmail, Long userId, Role role) {
        User admin = lookup.userByEmail(adminEmail);
        User user = lookup.userById(userId);
        if (admin.getId().equals(user.getId())) {
            throw ApiException.conflict("Administrators cannot change their own role.");
        }
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN
                && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw ApiException.conflict("The last active administrator cannot be demoted.");
        }
        user.setRole(role);
        audit.save(new AdminAudit(admin, "CHANGE_ROLE", "USER", userId));
        return userService.toResponse(user);
    }

    @Transactional
    public void disableUser(String adminEmail, Long userId) {
        User admin = lookup.userByEmail(adminEmail);
        User user = lookup.userById(userId);
        if (admin.getId().equals(user.getId())) {
            throw ApiException.conflict("Administrators cannot disable their own account.");
        }
        if (user.getRole() == Role.ADMIN && users.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw ApiException.conflict("The last active administrator cannot be disabled.");
        }
        user.setEnabled(false);
        audit.save(new AdminAudit(admin, "DISABLE_USER", "USER", userId));
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> reports(Pageable pageable) {
        return reports.list(pageable);
    }

    @Transactional
    public ReportResponse updateReportStatus(String adminEmail, Long reportId, ReportStatus status) {
        User admin = lookup.userByEmail(adminEmail);
        ReportResponse updated = reports.setStatus(reportId, status);
        audit.save(new AdminAudit(admin, "UPDATE_REPORT", "REPORT", reportId));
        return updated;
    }

    @Transactional
    public void removeReportedPost(String adminEmail, Long postId) {
        User admin = lookup.userByEmail(adminEmail);
        postsService.delete(adminEmail, postId);
        audit.save(new AdminAudit(admin, "REMOVE_REPORTED_POST", "POST", postId));
    }
}
