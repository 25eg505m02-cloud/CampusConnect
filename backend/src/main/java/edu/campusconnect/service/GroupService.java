package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.GroupRequest;
import edu.campusconnect.dto.ApiDtos.GroupResponse;
import edu.campusconnect.entity.CampusGroup;
import edu.campusconnect.entity.GroupMembership;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.CampusGroupRepository;
import edu.campusconnect.repository.GroupMembershipRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupService {
    private final CampusGroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final EntityLookup lookup;
    private final NotificationService notifications;

    public GroupService(CampusGroupRepository groups, GroupMembershipRepository memberships,
                        EntityLookup lookup, NotificationService notifications) {
        this.groups = groups;
        this.memberships = memberships;
        this.lookup = lookup;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Page<GroupResponse> list(Pageable pageable) {
        return groups.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GroupResponse get(Long id) {
        return toResponse(lookup.group(id));
    }

    @Transactional
    public GroupResponse create(String email, GroupRequest request) {
        User creator = lookup.userByEmail(email);
        CampusGroup group = new CampusGroup();
        group.setName(request.name().trim());
        group.setDescription(request.description().trim());
        group.setCreator(creator);
        CampusGroup saved = groups.save(group);
        memberships.save(new GroupMembership(saved, creator));
        return toResponse(saved);
    }

    @Transactional
    public void join(String email, Long groupId) {
        User user = lookup.userByEmail(email);
        CampusGroup group = lookup.group(groupId);
        if (memberships.existsByGroupIdAndUserId(groupId, user.getId())) {
            return;
        }
        memberships.save(new GroupMembership(group, user));
        notifications.notify(group.getCreator(), "GROUP_MEMBER",
                user.getFullName() + " joined " + group.getName() + ".", groupId);
    }

    @Transactional
    public void leave(String email, Long groupId) {
        User user = lookup.userByEmail(email);
        lookup.group(groupId);
        if (!memberships.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw ApiException.notFound("You are not a member of this group.");
        }
        if (groups.findById(groupId).orElseThrow().getCreator().getId().equals(user.getId())
                && memberships.countByGroupId(groupId) <= 1) {
            throw ApiException.conflict("The group creator cannot leave while they are its only member.");
        }
        memberships.deleteByGroupIdAndUserId(groupId, user.getId());
    }

    private GroupResponse toResponse(CampusGroup group) {
        return new GroupResponse(group.getId(), group.getName(), group.getDescription(),
                group.getCreator().getId(), group.getCreator().getFullName(),
                memberships.countByGroupId(group.getId()), group.getCreatedAt());
    }
}
