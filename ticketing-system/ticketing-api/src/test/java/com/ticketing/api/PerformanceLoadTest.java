package com.ticketing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.api.dto.BookingRequest;
import com.ticketing.domain.booking.BookingStatus;
import com.ticketing.domain.booking.repository.BookingRepository;
import com.ticketing.domain.event.repository.SeatGradeRepository;
import com.ticketing.infra.redis.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
public class PerformanceLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SeatGradeRepository seatGradeRepository;

    @MockBean
    private QueueService queueService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM booking");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM seat_grade");
        jdbcTemplate.execute("DELETE FROM event");

        jdbcTemplate.execute("INSERT INTO event (id, title, event_date, venue, status) VALUES (1, 'Test Concert', '2026-12-31 19:00:00', 'Seoul', 'OPEN')");
        jdbcTemplate.execute("INSERT INTO seat_grade (id, event_id, grade_name, price, total_count, remain_count) VALUES (1, 1, 'VIP', 100000, 500, 500)");

        String sql = "INSERT INTO users (id, email, name) VALUES (?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            batchArgs.add(new Object[]{ (long)i, "user" + i + "@test.com", "Tester" + i });
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);

        // QueueService Mocking
        when(queueService.isAdmitted(anyLong(), anyLong())).thenReturn(true);
        when(queueService.isAlreadyBooked(anyLong(), anyLong())).thenReturn(false);
        
        // 원자적 재고 감소 시뮬레이션 (500개까지만 성공)
        AtomicInteger mockStock = new AtomicInteger(500);
        when(queueService.decreaseStock(anyLong(), anyLong())).thenAnswer(invocation -> {
            int current = mockStock.get();
            if (current > 0) {
                return mockStock.decrementAndGet() >= 0;
            }
            return false;
        });
    }

    @Test
    @DisplayName("시나리오: 1,000명 동시 예매 시도 (재고 500개) - 비동기 처리 정합성 검증")
    void testPerformance() throws InterruptedException {
        int userCount = 1000;
        int stockCount = 500;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(userCount);
        
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= userCount; i++) {
            final long userId = i;
            executorService.execute(() -> {
                try {
                    BookingRequest request = new BookingRequest(userId, 1L, 1L); 
                    mockMvc.perform(post("/api/v1/bookings")
                            .param("userId", String.valueOf(userId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == 200) {
                                    successCount.incrementAndGet();
                                } else {
                                    failCount.incrementAndGet();
                                }
                            });
                } catch (Exception ignored) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        
        System.out.println("================ PERFORMANCE RESULT ================");
        System.out.println("Total Time: " + (endTime - startTime) + " ms");
        System.out.println("Success (200 OK): " + successCount.get());
        System.out.println("Fail (Sold Out/Error): " + failCount.get());
        System.out.println("Throughput (TPS): " + (userCount / ((endTime - startTime) / 1000.0)));
        System.out.println("====================================================");

        // Kafka Consumer의 비동기 처리를 위해 대기
        Thread.sleep(10000); 

        long confirmedCount = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
        
        int remainCount = seatGradeRepository.findById(1L).get().getRemainCount();

        System.out.println("DB Results -> Confirmed: " + confirmedCount + ", Remain: " + remainCount);

        // 최종 검증
        assertThat(successCount.get()).isEqualTo(stockCount); // API 성공 응답 수
        assertThat(confirmedCount).isEqualTo(stockCount);    // DB 최종 확정 수
        assertThat(remainCount).isZero();                    // DB 재고 소진
    }
}
