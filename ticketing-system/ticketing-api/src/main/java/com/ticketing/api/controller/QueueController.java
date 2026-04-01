package com.ticketing.api.controller;

import com.ticketing.domain.dto.QueueStatusResponse;
import com.ticketing.infra.redis.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping("/{eventId}/enter")
    public long enterQueue(@PathVariable Long eventId, @RequestParam Long userId) {
        return queueService.enterQueue(eventId, userId);
    }

    @GetMapping("/{eventId}/status")
    public QueueStatusResponse getStatus(@PathVariable Long eventId, @RequestParam Long userId) {
        return queueService.getStatus(eventId, userId);
    }
}
