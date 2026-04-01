CREATE TABLE booking (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    seat_grade_id   BIGINT NOT NULL,
    status          ENUM('PENDING','CONFIRMED','CANCELLED') DEFAULT 'PENDING',
    price           INT NOT NULL,
    booked_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (seat_grade_id) REFERENCES seat_grade(id)
);
