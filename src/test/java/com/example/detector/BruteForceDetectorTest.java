package com.example.detector;

import com.example.model.entity.AnomalyAlert;
import com.example.model.entity.UserActivity;
import com.example.model.enums.AnomalySeverity;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BruteForceDetectorTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @InjectMocks
    private BruteForceDetector detector;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(detector, "maxAttempts", 5);
        ReflectionTestUtils.setField(detector, "windowMinutes", 10);
    }

    @Test
    void shouldNotTriggerForNonFailedLoginEvent() {
        var event = buildEvent(EventType.LOGIN);
        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerWhenBelowThreshold() {
        var event = buildEvent(EventType.FAILED_LOGIN);
        when(userActivityRepository.findByUserIdAndEventTypeAndCreatedAtAfter(eq(1L), eq(EventType.FAILED_LOGIN), any()))
                .thenReturn(List.of(UserActivity.builder().build(), UserActivity.builder().build()));
        when(userActivityRepository.findByIpAddressAndEventTypeAndCreatedAtAfter(eq("1.2.3.4"), eq(EventType.FAILED_LOGIN), any()))
                .thenReturn(Collections.emptyList());

        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldTriggerWhenUserExceedsThreshold() {
        var event = buildEvent(EventType.FAILED_LOGIN);
        var failures = List.of(
                UserActivity.builder().build(),
                UserActivity.builder().build(),
                UserActivity.builder().build(),
                UserActivity.builder().build(),
                UserActivity.builder().build()
        );
        when(userActivityRepository.findByUserIdAndEventTypeAndCreatedAtAfter(eq(1L), eq(EventType.FAILED_LOGIN), any()))
                .thenReturn(failures);
        when(userActivityRepository.findByIpAddressAndEventTypeAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        var result = detector.detect(event, UserActivity.builder().build());

        assertThat(result).hasSize(1);
        AnomalyAlert alert = result.get(0);
        assertThat(alert.getAnomalyType()).isEqualTo(AnomalyType.BRUTE_FORCE);
        assertThat(alert.getSeverity()).isEqualTo(AnomalySeverity.HIGH);
        assertThat(alert.getUserId()).isEqualTo(1L);
    }

    @Test
    void shouldTriggerWhenIpExceedsThreshold() {
        var event = buildEvent(EventType.FAILED_LOGIN);
        when(userActivityRepository.findByUserIdAndEventTypeAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        var failures = List.of(
                UserActivity.builder().build(),
                UserActivity.builder().build(),
                UserActivity.builder().build(),
                UserActivity.builder().build(),
                UserActivity.builder().build()
        );
        when(userActivityRepository.findByIpAddressAndEventTypeAndCreatedAtAfter(eq("1.2.3.4"), eq(EventType.FAILED_LOGIN), any()))
                .thenReturn(failures);

        var result = detector.detect(event, UserActivity.builder().build());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnomalyType()).isEqualTo(AnomalyType.BRUTE_FORCE);
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
