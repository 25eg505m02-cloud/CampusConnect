package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.CommentRequest;
import edu.campusconnect.dto.ApiDtos.CommentResponse;
import edu.campusconnect.dto.ApiDtos.PostRequest;
import edu.campusconnect.dto.ApiDtos.PostResponse;
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
@RequestMapping("/api/v1")
public class PostController {
    private final PostService posts;

    public PostController(PostService posts) {
        this.posts = posts;
    }

    @GetMapping("/posts")
    public Page<PostResponse> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        return q == null ? posts.list(pageable) : posts.search(q, pageable);
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @AuthenticationPrincipal UserDetails currentUser, @Valid @RequestBody PostRequest request) {
        return posts.create(currentUser.getUsername(), request);
    }

    @GetMapping("/posts/{id}")
    public PostResponse get(@PathVariable Long id) {
        return posts.get(id);
    }

    @PatchMapping("/posts/{id}")
    public PostResponse update(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody PostRequest request) {
        return posts.update(currentUser.getUsername(), id, request);
    }

    @DeleteMapping("/posts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        posts.delete(currentUser.getUsername(), id);
    }

    @PostMapping("/posts/{id}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void like(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        posts.like(currentUser.getUsername(), id);
    }

    @DeleteMapping("/posts/{id}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        posts.unlike(currentUser.getUsername(), id);
    }

    @GetMapping("/posts/{id}/comments")
    public Page<CommentResponse> comments(
            @PathVariable Long id, @PageableDefault(size = 50) Pageable pageable) {
        return posts.comments(id, pageable);
    }

    @PostMapping("/posts/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest request) {
        return posts.addComment(currentUser.getUsername(), id, request);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal UserDetails currentUser, @PathVariable Long id) {
        posts.deleteComment(currentUser.getUsername(), id);
    }
}
