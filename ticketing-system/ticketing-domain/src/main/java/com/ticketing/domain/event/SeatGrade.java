package com.ticketing.domain.event;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "seat_grade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 50)
    private String gradeName;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int totalCount;

    @Column(nullable = false)
    private int remainCount;

    public SeatGrade(Event event, String gradeName, int price, int totalCount) {
        this.event = event;
        this.gradeName = gradeName;
        this.price = price;
        this.totalCount = totalCount;
        this.remainCount = totalCount;
    }
}
