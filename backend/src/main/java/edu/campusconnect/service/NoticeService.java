package edu.campusconnect.service;

import edu.campusconnect.dto.ApiDtos.NoticeRequest;
import edu.campusconnect.dto.ApiDtos.NoticeResponse;
import edu.campusconnect.entity.Notice;
import edu.campusconnect.entity.Role;
import edu.campusconnect.entity.User;
import edu.campusconnect.exception.ApiException;
import edu.campusconnect.repository.NoticeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoticeService {
    private final NoticeRepository notices;
    private final EntityLookup lookup;
    private final NotificationService notifications;

    public NoticeService(NoticeRepository notices, EntityLookup lookup, NotificationService notifications) {
        this.notices = notices;
        this.lookup = lookup;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public Page<NoticeResponse> list(Pageable pageable) {
        return notices.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public NoticeResponse get(Long id) {
        return toResponse(lookup.notice(id));
    }

    @Transactional
    public NoticeResponse create(String email, NoticeRequest request) {
        User author = lookup.userByEmail(email);
        requirePublisher(author);
        Notice notice = new Notice();
        notice.setTitle(request.title().trim());
        notice.setContent(request.content().trim());
        notice.setCategory(request.category() == null ? null : request.category().trim());
        notice.setAuthor(author);
        Notice saved = notices.save(notice);
        notifications.notifyAllActive("CAMPUS_NOTICE",
                "New campus notice: " + saved.getTitle(), saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public NoticeResponse update(String email, Long id, NoticeRequest request) {
        User author = lookup.userByEmail(email);
        Notice notice = lookup.notice(id);
        if (!notice.getAuthor().getId().equals(author.getId()) && author.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only the notice author or an administrator may edit this notice.");
        }
        notice.setTitle(request.title().trim());
        notice.setContent(request.content().trim());
        notice.setCategory(request.category() == null ? null : request.category().trim());
        return toResponse(notice);
    }

    @Transactional
    public void delete(String email, Long id) {
        User author = lookup.userByEmail(email);
        Notice notice = lookup.notice(id);
        if (!notice.getAuthor().getId().equals(author.getId()) && author.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only the notice author or an administrator may delete this notice.");
        }
        notices.delete(notice);
    }

    private void requirePublisher(User user) {
        if (user.getRole() != Role.FACULTY && user.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Only faculty and administrators can publish notices.");
        }
    }

    private NoticeResponse toResponse(Notice notice) {
        return new NoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(),
                notice.getCategory(), notice.getAuthor().getId(), notice.getAuthor().getFullName(),
                notice.getCreatedAt(), notice.getUpdatedAt());
    }
}
