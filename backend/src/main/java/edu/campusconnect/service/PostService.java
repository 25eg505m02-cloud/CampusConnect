package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.CommentRequest;
import edu.campusconnect.dto.ApiDtos.CommentResponse;
import edu.campusconnect.dto.ApiDtos.PostRequest;
import edu.campusconnect.dto.ApiDtos.PostResponse;
import edu.campusconnect.entity.Comment;
import edu.campusconnect.entity.Post;
import edu.campusconnect.entity.PostLike;
import edu.campusconnect.entity.Role;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository posts;
    private final CommentRepository comments;
    private final PostLikeRepository likes;
    private final GroupMembershipRepository memberships;
    private final EntityLookup lookup;
    private final NotificationService notifications;

    public PostService(PostRepository posts, CommentRepository comments, PostLikeRepository likes,
                       GroupMembershipRepository memberships, EntityLookup lookup,
                       NotificationService notifications) {
        this.posts = posts;
        this.comments = comments;
        this.likes = likes;
        this.memberships = memberships;
        this.lookup = lookup;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> list(Pageable pageable) {
        return posts.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> search(String query, Pageable pageable) {
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            return list(pageable);
        }
        return posts.findByContentContainingIgnoreCaseOrderByCreatedAtDesc(term, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PostResponse get(Long id) {
        return toResponse(lookup.post(id));
    }

    @Transactional
    public PostResponse create(String email, PostRequest request) {
        User author = lookup.userByEmail(email);
        Post post = new Post();
        post.setAuthor(author);
        post.setContent(request.content().trim());
        if (request.groupId() != null) {
            var group = lookup.group(request.groupId());
            if (!memberships.existsByGroupIdAndUserId(group.getId(), author.getId())) {
                throw ApiException.forbidden("Join the group before posting to it.");
            }
            post.setGroup(group);
        }
        return toResponse(posts.save(post));
    }

    @Transactional
    public PostResponse update(String email, Long postId, PostRequest request) {
        User user = lookup.userByEmail(email);
        Post post = lookup.post(postId);
        requirePostOwnerOrAdmin(user, post);
        post.setContent(request.content().trim());
        if (request.groupId() == null) {
            post.setGroup(null);
        } else {
            var group = lookup.group(request.groupId());
            if (!memberships.existsByGroupIdAndUserId(group.getId(), user.getId())) {
                throw ApiException.forbidden("Join the group before posting to it.");
            }
            post.setGroup(group);
        }
        return toResponse(post);
    }

    @Transactional
    public void delete(String email, Long postId) {
        User user = lookup.userByEmail(email);
        Post post = lookup.post(postId);
        requirePostOwnerOrAdmin(user, post);
        comments.deleteByPostId(postId);
        likes.deleteByPostId(postId);
        posts.delete(post);
    }

    @Transactional
    public void like(String email, Long postId) {
        User user = lookup.userByEmail(email);
        Post post = lookup.post(postId);
        if (!likes.existsByPostIdAndUserId(postId, user.getId())) {
            likes.save(new PostLike(post, user));
            if (!post.getAuthor().getId().equals(user.getId())) {
                notifications.notify(post.getAuthor(), "POST_LIKE",
                        user.getFullName() + " liked your post.", postId);
            }
        }
    }

    @Transactional
    public void unlike(String email, Long postId) {
        User user = lookup.userByEmail(email);
        lookup.post(postId);
        likes.deleteByPostIdAndUserId(postId, user.getId());
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> comments(Long postId, Pageable pageable) {
        lookup.post(postId);
        return comments.findByPostIdOrderByCreatedAtAsc(postId, pageable).map(this::toCommentResponse);
    }

    @Transactional
    public CommentResponse addComment(String email, Long postId, CommentRequest request) {
        User user = lookup.userByEmail(email);
        Post post = lookup.post(postId);
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(user);
        comment.setContent(request.content().trim());
        Comment saved = comments.save(comment);
        if (!post.getAuthor().getId().equals(user.getId())) {
            notifications.notify(post.getAuthor(), "POST_COMMENT",
                    user.getFullName() + " commented on your post.", postId);
        }
        return toCommentResponse(saved);
    }

    @Transactional
    public void deleteComment(String email, Long commentId) {
        User user = lookup.userByEmail(email);
        Comment comment = lookup.comment(commentId);
        if (!comment.getAuthor().getId().equals(user.getId())
                && !comment.getPost().getAuthor().getId().equals(user.getId())
                && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("You cannot delete this comment.");
        }
        comments.delete(comment);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> byGroup(Long groupId, Pageable pageable) {
        lookup.group(groupId);
        return posts.findByGroupIdOrderByCreatedAtDesc(groupId, pageable).map(this::toResponse);
    }

    private void requirePostOwnerOrAdmin(User user, Post post) {
        if (!post.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("You can only modify your own posts.");
        }
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(post.getId(), post.getContent(), post.getAuthor().getId(),
                post.getAuthor().getFullName(),
                post.getGroup() == null ? null : post.getGroup().getId(),
                post.getGroup() == null ? null : post.getGroup().getName(),
                likes.countByPostId(post.getId()), comments.countByPostId(post.getId()),
                post.getCreatedAt(), post.getUpdatedAt());
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getPost().getId(),
                comment.getAuthor().getId(), comment.getAuthor().getFullName(),
                comment.getContent(), comment.getCreatedAt());
    }
}
