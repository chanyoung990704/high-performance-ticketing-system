CREATE TABLE queue_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    event_id        BIGINT NOT NULL,
    queue_entered_at DATETIME NOT NULL,
    queue_exited_at  DATETIME,
    wait_seconds    INT,
    result          ENUM('BOOKING_SUCCESS','BOOKING_FAILED','TIMEOUT','CANCELLED')
);
