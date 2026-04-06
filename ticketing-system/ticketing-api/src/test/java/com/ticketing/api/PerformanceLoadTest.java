package com.ticketing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.api.dto.BookingEvent;
import com.ticketing.api.dto.BookingRequest;
import com.ticketing.domain.booking.Booking;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(TestRedisConfig.class)
@EmbeddedKafka(partitions = 9, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
public class PerformanceLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private BookingRepository bookingRepository;

    @SpyBean
    private SeatGradeRepository seatGradeRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private QueueService queueService;

    @Autowired
    private com.ticketing.api.service.BookingService bookingService;

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
        for (int i = 1; i <= 10000; i++) {
            batchArgs.add(new Object[]{(long) i, "user" + i + "@test.com", "Tester" + i});
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);

        when(queueService.isAdmitted(anyLong(), anyLong())).thenReturn(true);
        when(queueService.isAlreadyBooked(anyLong(), anyLong())).thenReturn(false);
    }

    @Test
    @DisplayName("시나리오 1: 대기열 진입 부하 테스트 (10,000명 동시)")
    void testScenario1_QueueEnter() throws InterruptedException {
        int userCount = 10000;
        ExecutorService executorService = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(userCount);
        when(queueService.enterQueue(anyLong(), anyLong())).thenReturn(0L);

        long startTime = System.currentTimeMillis();
        for (int i = 1; i <= userCount; i++) {
            final long userId = i;
            executorService.execute(() -> {
                try {
                    String content = "{\"userId\": " + userId + "}";
                    mockMvc.perform(post("/api/v1/queues/1/enter")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(content));
                } catch (Exception ignored) {} finally { latch.countDown(); }
            });
        }
        latch.await();
        long duration = System.currentTimeMillis() - startTime;
        System.out.println(">>> [SCENARIO 1] TPS: " + (userCount / (duration / 1000.0)));
    }

    @Test
    @DisplayName("시나리오 2: 동시 예매 부하 테스트 (재고 500, 요청 10,000)")
    void testScenario2_BookingPerformance() throws InterruptedException {
        int userCount = 10000;
        int stockCount = 500;
        ExecutorService executorService = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(userCount);
        AtomicInteger successCount = new AtomicInteger();
        
        AtomicInteger mockStock = new AtomicInteger(500);
        when(queueService.decreaseStock(anyLong(), anyLong())).thenAnswer(invocation -> {
            int current = mockStock.get();
            if (current > 0) {
                return mockStock.decrementAndGet() >= 0;
            }
            return false;
        });

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
                            .andDo(result -> { if (result.getResponse().getStatus() == 200) successCount.incrementAndGet(); });
                } catch (Exception ignored) {} finally { latch.countDown(); }
            });
        }
        latch.await();
        long requestDuration = System.currentTimeMillis() - startTime;

        System.out.println(">>> [SCENARIO 2] REQUEST TPS: " + (userCount / (requestDuration / 1000.0)));
        System.out.println("Wait for Async Kafka processing (20s)...");
        Thread.sleep(20000); 

        long confirmedCount = bookingRepository.findAll().stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        System.out.println(">>> [SCENARIO 2] Success Response: " + successCount.get() + ", Final Confirmed in DB: " + confirmedCount);
        
        assertThat(successCount.get()).isEqualTo(stockCount);
        assertThat(confirmedCount).isEqualTo(stockCount);
    }

    @Test
    @DisplayName("시나리오 3: DB 저장 실패 시 Redis 재고 복구 검증")
    void testScenario3_Rollback() {
        Long userId = 1L;
        BookingRequest request = new BookingRequest(userId, 1L, 1L);
        when(queueService.decreaseStock(anyLong(), anyLong())).thenReturn(true);
        doThrow(new RuntimeException("DB Error")).when(bookingRepository).save(any());

        try { bookingService.createBooking(userId, request); } catch (Exception ignored) {}

        verify(queueService, times(1)).increaseStock(eq(1L), eq(1L));
        System.out.println(">>> [SCENARIO 3] Rollback Recovery Verified.");
    }

    @Test
    @DisplayName("시나리오 4: DLQ 전송 및 FAILED 상태 변경 검증")
    void testScenario4_Dlq() throws Exception {
        Long bookingId = 999L;
        doThrow(new RuntimeException("Fatal")).when(seatGradeRepository).decreaseRemainCount(anyLong());

        jdbcTemplate.execute("INSERT INTO booking (id, user_id, seat_grade_id, status, price) VALUES (999, 1, 1, 'PENDING', 10000)");

        BookingEvent event = BookingEvent.of(bookingId, 1L, 1L, 1L);
        kafkaTemplate.send("booking-created", "1", objectMapper.writeValueAsString(event));

        Thread.sleep(10000); 

        Booking updated = bookingRepository.findById(bookingId).orElseThrow();
        System.out.println(">>> [SCENARIO 4] Updated Status: " + updated.getStatus());
        assertThat(updated.getStatus()).isEqualTo(BookingStatus.FAILED);
    }
}
