package com.ticketing.api;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TicketingApiApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void contextLoads() {
        // 컨텍스트 로드 테스트
    }

    @Test
    void databaseConnectionTest() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection).isNotNull();
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("MySQL");
        }
    }

    @Test
    void redisConnectionTest() {
        redisTemplate.opsForValue().set("test-key", "test-value");
        String value = redisTemplate.opsForValue().get("test-key");
        assertThat(value).isEqualTo("test-value");
    }

    @Test
    void kafkaConnectionTest() {
        // Kafka 연결 확인 (간단히 템플릿 로드 여부만 확인하거나 메타데이터 조회)
        assertThat(kafkaTemplate).isNotNull();
    }
}
