package edu.campusconnect.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "event_registrations", uniqueConstraints = @UniqueConstraint(name = "uk_event_registration", columnNames = {"event_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class EventRegistration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant registeredAt = Instant.now();

    public EventRegistration(Event event, User user) {
        this.event = event;
        this.user = user;
    }
}
