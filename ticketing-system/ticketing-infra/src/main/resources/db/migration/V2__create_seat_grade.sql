CREATE TABLE seat_grade (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id    BIGINT NOT NULL,
    grade_name  VARCHAR(50) NOT NULL,
    price       INT NOT NULL,
    total_count INT NOT NULL,
    remain_count INT NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event(id)
);
