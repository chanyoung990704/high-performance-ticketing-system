package com.ticketing.domain.dto;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueStatusResponse {

    private String status; // ADMITTED, WAITING, NOT_FOUND
    private Long rank;
    private Long estimatedWaitSeconds;

    public static QueueStatusResponse admitted() {
        QueueStatusResponse response = new QueueStatusResponse();
        response.status = "ADMITTED";
        response.rank = 0L;
        response.estimatedWaitSeconds = 0L;
        return response;
    }

    public static QueueStatusResponse waiting(Long rank, Long estimatedWaitSeconds) {
        QueueStatusResponse response = new QueueStatusResponse();
        response.status = "WAITING";
        response.rank = rank;
        response.estimatedWaitSeconds = estimatedWaitSeconds;
        return response;
    }

    public static QueueStatusResponse notFound() {
        QueueStatusResponse response = new QueueStatusResponse();
        response.status = "NOT_FOUND";
        return response;
    }
}
