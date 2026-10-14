package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.NoticeRequest;
import edu.campusconnect.dto.ApiDtos.NoticeResponse;
import edu.campusconnect.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {
    private final NoticeService notices;

    public NoticeController(NoticeService notices) {
        this.notices = notices;
    }

    @GetMapping
    public Page<NoticeResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return notices.list(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoticeResponse create(
            @AuthenticationPrincipal UserDetails currentUser, @Valid @RequestBody NoticeRequest request) {
        return notices.create(currentUser.getUsername(), request);
    }

    @GetMapping("/{id}")
    public NoticeResponse get(@PathVariable Long id) {
        return notices.get(id);
    }

    @PatchMapping("/{id}")
    public NoticeResponse update(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody NoticeRequest request) {
        return notices.update(currentUser.getUsername(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        notices.delete(currentUser.getUsername(), id);
    }
}
