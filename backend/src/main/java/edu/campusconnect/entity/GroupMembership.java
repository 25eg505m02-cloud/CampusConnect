package edu.campusconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "group_memberships", uniqueConstraints = @UniqueConstraint(name = "uk_group_membership", columnNames = {"group_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class GroupMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private CampusGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    public GroupMembership(CampusGroup group, User user) {
        this.group = group;
        this.user = user;
    }
}
