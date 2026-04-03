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
    }

    @Test
    @DisplayName("시나리오 1: 대기열 진입 부하 테스트 (1,000명 동시)")
    void testQueueEnterPerformance() throws InterruptedException {
        int userCount = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(userCount);
        
        // enterQueue는 long(rank) 반환
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
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        long duration = System.currentTimeMillis() - startTime;

        System.out.println(">>> [SCENARIO 1] QUEUE ENTER RESULT <<<");
        System.out.println("Total Requests: 1000");
        System.out.println("Duration: " + duration + " ms");
        System.out.println("TPS: " + (userCount / (duration / 1000.0)));
        System.out.println("Average Response Time: " + (duration / (double)userCount) + " ms");
    }

    @Test
    @DisplayName("시나리오 2: 동시 예매 부하 테스트 (재고 500, 요청 1,000)")
    void testBookingPerformance() throws InterruptedException {
        int userCount = 1000;
        int stockCount = 500;
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(userCount);
        
        AtomicInteger successCount = new AtomicInteger();
        
        // 필수 Mocking
        when(queueService.isAdmitted(anyLong(), anyLong())).thenReturn(true);
        when(queueService.isAlreadyBooked(anyLong(), anyLong())).thenReturn(false);
        
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
                            .andDo(result -> {
                                if (result.getResponse().getStatus() == 200) {
                                    successCount.incrementAndGet();
                                }
                            });
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        long duration = System.currentTimeMillis() - startTime;

        System.out.println(">>> [SCENARIO 2] BOOKING PROCESS START <<<");
        System.out.println("Wait for Async Kafka processing...");
        Thread.sleep(10000); 

        long confirmedCount = bookingRepository.findAll().stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        int remainCount = seatGradeRepository.findById(1L).get().getRemainCount();

        System.out.println(">>> [SCENARIO 2] BOOKING RESULT <<<");
        System.out.println("Total Requests: 1000 (Target Stock: 500)");
        System.out.println("Duration (Requesting): " + duration + " ms");
        System.out.println("TPS: " + (userCount / (duration / 1000.0)));
        System.out.println("Confirmed in DB: " + confirmedCount + ", Remaining Stock: " + remainCount);

        assertThat(successCount.get()).isEqualTo(stockCount);
        assertThat(confirmedCount).isEqualTo(stockCount);
        assertThat(remainCount).isZero();
    }
}
