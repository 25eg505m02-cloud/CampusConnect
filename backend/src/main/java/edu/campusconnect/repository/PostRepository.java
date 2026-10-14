package edu.campusconnect.repository;

import edu.campusconnect.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);
    Page<Post> findByGroupIdOrderByCreatedAtDesc(Long groupId, Pageable pageable);
    Page<Post> findByContentContainingIgnoreCaseOrderByCreatedAtDesc(String query, Pageable pageable);
    Optional<Post> findByIdAndGroupId(Long id, Long groupId);
    long countByAuthorId(Long authorId);
}
