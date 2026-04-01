package com.ticketing.domain.queue;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "queue_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QueueHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private LocalDateTime queueEnteredAt;

    private LocalDateTime queueExitedAt;

    private Integer waitSeconds;

    @Enumerated(EnumType.STRING)
    private QueueResult result;

    public QueueHistory(Long userId, Long eventId, LocalDateTime queueEnteredAt) {
        this.userId = userId;
        this.eventId = eventId;
        this.queueEnteredAt = queueEnteredAt;
    }

    public void complete(LocalDateTime queueExitedAt, Integer waitSeconds, QueueResult result) {
        this.queueExitedAt = queueExitedAt;
        this.waitSeconds = waitSeconds;
        this.result = result;
    }
}
