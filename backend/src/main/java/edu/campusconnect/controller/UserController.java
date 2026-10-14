package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.UpdateProfileRequest;
import edu.campusconnect.dto.ApiDtos.UserResponse;
import edu.campusconnect.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return users.get(id);
    }

    @PatchMapping("/me")
    public UserResponse update(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return users.update(currentUser.getUsername(), request);
    }

    @GetMapping("/search")
    public Page<UserResponse> search(
            @RequestParam @NotBlank String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return users.search(q, pageable);
    }

    @GetMapping("/me/connections")
    public List<UserResponse> connections(@AuthenticationPrincipal UserDetails currentUser) {
        return users.connections(currentUser.getUsername());
    }

    @GetMapping("/me/connection-requests")
    public List<UserResponse> incomingConnectionRequests(@AuthenticationPrincipal UserDetails currentUser) {
        return users.incomingConnectionRequests(currentUser.getUsername());
    }

    @PostMapping("/{id}/connections")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestConnection(
            @AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        users.requestConnection(currentUser.getUsername(), id);
    }

    @PatchMapping("/me/connections/{requesterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptConnection(
            @AuthenticationPrincipal UserDetails currentUser, @PathVariable Long requesterId) {
        users.acceptConnection(currentUser.getUsername(), requesterId);
    }

    @DeleteMapping("/me/connections/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeConnection(
            @AuthenticationPrincipal UserDetails currentUser, @PathVariable Long userId) {
        users.removeConnection(currentUser.getUsername(), userId);
    }
}
