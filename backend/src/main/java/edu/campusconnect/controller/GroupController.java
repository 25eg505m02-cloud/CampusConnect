package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.GroupRequest;
import edu.campusconnect.dto.ApiDtos.GroupResponse;
import edu.campusconnect.dto.ApiDtos.PostResponse;
import edu.campusconnect.service.GroupService;
import edu.campusconnect.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {
    private final GroupService groups;
    private final PostService posts;

    public GroupController(GroupService groups, PostService posts) {
        this.groups = groups;
        this.posts = posts;
    }

    @GetMapping
    public Page<GroupResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return groups.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(
            @AuthenticationPrincipal UserDetails currentUser, @Valid @RequestBody GroupRequest request) {
        return groups.create(currentUser.getUsername(), request);
    }

    @GetMapping("/{id}")
    public GroupResponse get(@PathVariable Long id) {
        return groups.get(id);
    }

    @GetMapping("/{id}/posts")
    public Page<PostResponse> posts(
            @PathVariable Long id, @PageableDefault(size = 20) Pageable pageable) {
        return posts.byGroup(id, pageable);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        groups.join(currentUser.getUsername(), id);
    }

    @DeleteMapping("/{id}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        groups.leave(currentUser.getUsername(), id);
    }
}
