package com.ticketing.api;

import com.ticketing.api.dto.BookingRequest;
import com.ticketing.api.dto.BookingResponse;
import com.ticketing.api.service.BookingService;
import com.ticketing.domain.booking.Booking;
import com.ticketing.domain.dto.QueueStatusResponse;
import com.ticketing.domain.event.Event;
import com.ticketing.domain.event.EventStatus;
import com.ticketing.domain.event.SeatGrade;
import com.ticketing.domain.event.repository.EventRepository;
import com.ticketing.domain.event.repository.SeatGradeRepository;
import com.ticketing.infra.redis.QueueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private QueueService queueService;
    @Autowired private EventRepository eventRepository;
    @Autowired private SeatGradeRepository seatGradeRepository;
    @Autowired private StringRedisTemplate redisTemplate;
    @org.springframework.boot.test.mock.mockito.MockBean
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    private Long eventId;
    private Long gradeId;
    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        
        Event event = eventRepository.save(new Event("테스트 공연", LocalDateTime.now().plusDays(1), "고척돔", EventStatus.OPEN));
        eventId = event.getId();
        
        SeatGrade seatGrade = seatGradeRepository.save(new SeatGrade(event, "VIP", 150000, 10));
        gradeId = seatGrade.getId();
        
        queueService.setStock(eventId, gradeId, 10);
    }

    @Test
    @DisplayName("1. 대기열 진입 API 테스트")
    void enterQueueApiTest() throws Exception {
        mockMvc.perform(post("/api/v1/queues/{eventId}/enter", eventId)
                .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @DisplayName("2. 대기 순위 조회 API 테스트")
    void getStatusApiTest() throws Exception {
        queueService.enterQueue(eventId, userId);
        
        mockMvc.perform(get("/api/v1/queues/{eventId}/status", eventId)
                .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.rank").value(1));
    }

    @Test
    @DisplayName("3. 미입장 시 예매 시도 (403 Forbidden) 테스트")
    void unauthorizedBookingTest() throws Exception {
        BookingRequest request = new BookingRequest(); // Using reflection or proper DTO setter would be better, but assuming current structure
        // Since BookingRequest has no setters, I'll use json string directly
        String json = String.format("{\"eventId\": %d, \"gradeId\": %d}", eventId, gradeId);

        mockMvc.perform(post("/api/v1/bookings")
                .param("userId", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("QUEUE_NOT_ADMITTED"));
    }

    @Test
    @DisplayName("4. 정상 예매 프로세스 통합 테스트 (진입 -> 승인 -> 예매)")
    void fullBookingFlowTest() throws Exception {
        // 1. 대기열 진입
        queueService.enterQueue(eventId, userId);
        
        // 2. 관리자/스케줄러에 의해 승인 처리 (테스트를 위해 직접 호출)
        queueService.processQueue(eventId, 1);
        
        // 디버깅: 입장 허가 여부 직접 확인
        boolean admitted = queueService.isAdmitted(eventId, userId);
        System.out.println(">>> User admitted: " + admitted);
        System.out.println(">>> Event ID: " + eventId + ", User ID: " + userId);
        
        // 3. 예매 시도
        String json = String.format("{\"eventId\": %d, \"gradeId\": %d}", eventId, gradeId);
        mockMvc.perform(post("/api/v1/bookings")
                .param("userId", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
                
        // 4. 예매 결과 조회
        // 이 부분은 bookingId를 가져와야 하므로 실제 응답을 파싱하거나 Service로 검증
    }

    @Test
    @DisplayName("5. 매진 시 예매 시도 (409 Conflict) 테스트")
    void soldOutTest() throws Exception {
        // 1. 재고를 0으로 설정
        queueService.setStock(eventId, gradeId, 0);
        queueService.enterQueue(eventId, userId);
        queueService.processQueue(eventId, 1);

        String json = String.format("{\"eventId\": %d, \"gradeId\": %d}", eventId, gradeId);
        mockMvc.perform(post("/api/v1/bookings")
                .param("userId", String.valueOf(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOLD_OUT"));
    }
}
