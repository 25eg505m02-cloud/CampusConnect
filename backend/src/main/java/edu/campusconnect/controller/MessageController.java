package edu.campusconnect.controller;

import edu.campusconnect.dto.ApiDtos.ConversationRequest;
import edu.campusconnect.dto.ApiDtos.ConversationResponse;
import edu.campusconnect.dto.ApiDtos.MessageRequest;
import edu.campusconnect.dto.ApiDtos.MessageResponse;
import edu.campusconnect.service.MessageService;
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
public class MessageController {
    private final MessageService messages;

    public MessageController(MessageService messages) {
        this.messages = messages;
    }

    @GetMapping("/conversations")
    public Page<ConversationResponse> conversations(
            @AuthenticationPrincipal UserDetails currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        return messages.list(currentUser.getUsername(), pageable);
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createConversation(
            @AuthenticationPrincipal UserDetails currentUser,
            @Valid @RequestBody ConversationRequest request) {
        return messages.create(currentUser.getUsername(), request.participantIds());
    }

    @GetMapping("/conversations/{id}/messages")
    public Page<MessageResponse> messages(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable Long id,
            @PageableDefault(size = 50) Pageable pageable) {
        return messages.messages(currentUser.getUsername(), id, pageable);
    }

    @PostMapping("/conversations/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(
            @AuthenticationPrincipal UserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody MessageRequest request) {
        return messages.send(currentUser.getUsername(), id, request);
    }
}
