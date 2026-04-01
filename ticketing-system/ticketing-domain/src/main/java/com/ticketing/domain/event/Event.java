package com.ticketing.domain.event;

import com.ticketing.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column(length = 200)
    private String venue;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    public Event(String title, LocalDateTime eventDate, String venue, EventStatus status) {
        this.title = title;
        this.eventDate = eventDate;
        this.venue = venue;
        this.status = status;
    }
}
