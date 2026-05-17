package com.example.detector;

import com.example.model.entity.UserActivity;
import com.example.model.enums.AnomalyType;
import com.example.model.event.EventType;
import com.example.model.event.UserEventMessage;
import com.example.repository.UserActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnusualLoginFrequencyDetectorTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @InjectMocks
    private UnusualLoginFrequencyDetector detector;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(detector, "windowDays", 30);
        ReflectionTestUtils.setField(detector, "multiplier", 3.0);
        ReflectionTestUtils.setField(detector, "minHistoryLogins", 5);
    }

    @Test
    void shouldNotTriggerForNonLoginEvent() {
        var event = buildEvent(EventType.FAILED_LOGIN);
        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerWhenInsufficientHistory() {
        var event = buildEvent(EventType.LOGIN);
        // Текущий час: 10, историческая база слишком маленькая
        when(userActivityRepository.countByUserIdAndEventTypeAndCreatedAtAfter(eq(1L), eq(EventType.LOGIN), any()))
                .thenReturn(10L)  // текущий час
                .thenReturn(3L);  // за 30 дней — меньше minHistoryLogins=5

        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerWhenFrequencyNormal() {
        var event = buildEvent(EventType.LOGIN);
        // Среднее: 720 логинов / (30*24=720 часов) = 1 логин/час
        // Текущий час: 2 — норма (ниже 1 * 3.0 = 3)
        when(userActivityRepository.countByUserIdAndEventTypeAndCreatedAtAfter(eq(1L), eq(EventType.LOGIN), any()))
                .thenReturn(2L)    // текущий час
                .thenReturn(720L); // за 30 дней

        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldTriggerWhenFrequencyAnomalous() {
        var event = buildEvent(EventType.LOGIN);
        // Среднее: 720 / 720 = 1 логин/час, порог = 3
        // Текущий час: 10 > 3 → алерт
        when(userActivityRepository.countByUserIdAndEventTypeAndCreatedAtAfter(eq(1L), eq(EventType.LOGIN), any()))
                .thenReturn(10L)   // текущий час
                .thenReturn(720L); // за 30 дней

        var result = detector.detect(event, UserActivity.builder().build());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnomalyType()).isEqualTo(AnomalyType.UNUSUAL_LOGIN_FREQUENCY);
        assertThat(result.get(0).getDescription()).contains("10 logins in the last hour");
    }

    private UserEventMessage buildEvent(EventType type) {
        return UserEventMessage.builder()
                .userId(1L)
                .eventType(type)
                .ipAddress("1.2.3.4")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
