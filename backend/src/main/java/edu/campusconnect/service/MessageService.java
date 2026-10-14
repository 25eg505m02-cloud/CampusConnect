package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.ConversationResponse;
import edu.campusconnect.dto.ApiDtos.MessageRequest;
import edu.campusconnect.dto.ApiDtos.MessageResponse;
import edu.campusconnect.dto.ApiDtos.UserResponse;
import edu.campusconnect.entity.ConnectionStatus;
import edu.campusconnect.entity.Conversation;
import edu.campusconnect.entity.ConversationMember;
import edu.campusconnect.entity.Message;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.ConversationMemberRepository;
import edu.campusconnect.repository.ConversationRepository;
import edu.campusconnect.repository.MessageRepository;
import edu.campusconnect.repository.UserConnectionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class MessageService {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;
    private final MessageRepository messages;
    private final UserConnectionRepository connections;
    private final EntityLookup lookup;
    private final UserService userService;
    private final NotificationService notifications;

    public MessageService(ConversationRepository conversations, ConversationMemberRepository members,
                         MessageRepository messages, UserConnectionRepository connections,
                         EntityLookup lookup, UserService userService, NotificationService notifications) {
        this.conversations = conversations;
        this.members = members;
        this.messages = messages;
        this.connections = connections;
        this.lookup = lookup;
        this.userService = userService;
        this.notifications = notifications;
    }

    @Transactional
    public ConversationResponse create(String email, List<Long> requestedParticipantIds) {
        User creator = lookup.userByEmail(email);
        LinkedHashSet<Long> participantIds = new LinkedHashSet<>(requestedParticipantIds);
        participantIds.add(creator.getId());
        if (participantIds.size() < 2) {
            throw ApiException.badRequest("A conversation needs at least one other participant.");
        }

        List<User> participants = new ArrayList<>();
        for (Long participantId : participantIds) {
            User participant = lookup.userById(participantId);
            if (!participant.getId().equals(creator.getId()) && !areConnected(creator.getId(), participant.getId())) {
                throw ApiException.forbidden("You can start a conversation only with accepted connections.");
            }
            participants.add(participant);
        }

        Conversation conversation = new Conversation();
        conversation.setCreatedBy(creator);
        Conversation saved = conversations.save(conversation);
        participants.forEach(user -> members.save(new ConversationMember(saved, user)));
        return toConversationResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponse> list(String email, Pageable pageable) {
        User user = lookup.userByEmail(email);
        return members.findByUserId(user.getId(), pageable).map(member -> {
            requireMember(member.getConversation().getId(), user.getId());
            return toConversationResponse(member.getConversation());
        });
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> messages(String email, Long conversationId, Pageable pageable) {
        User user = lookup.userByEmail(email);
        requireMember(conversationId, user.getId());
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable)
                .map(this::toMessageResponse);
    }

    @Transactional
    public MessageResponse send(String email, Long conversationId, MessageRequest request) {
        User sender = lookup.userByEmail(email);
        requireMember(conversationId, sender.getId());
        Conversation conversation = lookup.conversation(conversationId);
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.content().trim());
        Message saved = messages.save(message);
        members.findByConversationId(conversationId).stream()
                .map(ConversationMember::getUser)
                .filter(user -> !user.getId().equals(sender.getId()))
                .forEach(user -> notifications.notify(user, "MESSAGE",
                        sender.getFullName() + " sent you a message.", conversationId));
        return toMessageResponse(saved);
    }

    private void requireMember(Long conversationId, Long userId) {
        if (!members.existsByConversationIdAndUserId(conversationId, userId)) {
            throw ApiException.forbidden("You are not a member of this conversation.");
        }
    }

    private boolean areConnected(Long firstUserId, Long secondUserId) {
        return connections.findByRequesterIdAndRecipientId(firstUserId, secondUserId)
                .or(() -> connections.findByRequesterIdAndRecipientId(secondUserId, firstUserId))
                .map(connection -> connection.getStatus() == ConnectionStatus.ACCEPTED)
                .orElse(false);
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        List<UserResponse> participants = members.findByConversationId(conversation.getId()).stream()
                .map(ConversationMember::getUser)
                .map(userService::toResponse)
                .toList();
        Instant lastMessageAt = messages.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .map(Message::getCreatedAt)
                .orElse(conversation.getCreatedAt());
        return new ConversationResponse(conversation.getId(), participants, conversation.getCreatedAt(), lastMessageAt);
    }

    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(message.getId(), message.getConversation().getId(),
                message.getSender().getId(), message.getSender().getFullName(),
                message.getContent(), message.getCreatedAt());
    }
}
