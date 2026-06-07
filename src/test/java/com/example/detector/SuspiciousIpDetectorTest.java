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
class SuspiciousIpDetectorTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @InjectMocks
    private SuspiciousIpDetector detector;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(detector, "maxUsers", 3);
        ReflectionTestUtils.setField(detector, "windowMinutes", 60);
    }

    @Test
    void shouldNotTriggerForRegisterEvent() {
        var event = buildEvent(EventType.REGISTER, "1.2.3.4");
        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerForBlankIp() {
        var event = buildEvent(EventType.LOGIN, "");
        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerWhenFewUsers() {
        var event = buildEvent(EventType.LOGIN, "1.2.3.4");
        when(userActivityRepository.countDistinctUsersByIpAddressAndCreatedAtAfter(eq("1.2.3.4"), any()))
                .thenReturn(2L);

        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldTriggerWhenThresholdReached() {
        var event = buildEvent(EventType.LOGIN, "1.2.3.4");
        when(userActivityRepository.countDistinctUsersByIpAddressAndCreatedAtAfter(eq("1.2.3.4"), any()))
                .thenReturn(3L);

        var result = detector.detect(event, UserActivity.builder().build());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getAnomalyType()).isEqualTo(AnomalyType.SUSPICIOUS_IP);
        assertThat(result.getFirst().getIpAddress()).isEqualTo("1.2.3.4");
    }

    @Test
    void shouldTriggerForFailedLoginEventToo() {
        var event = buildEvent(EventType.FAILED_LOGIN, "5.6.7.8");
        when(userActivityRepository.countDistinctUsersByIpAddressAndCreatedAtAfter(eq("5.6.7.8"), any()))
                .thenReturn(5L);

        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).hasSize(1);
    }

    private UserEventMessage buildEvent(EventType type, String ip) {
        return UserEventMessage.builder()
                .userId(1L)
                .eventType(type)
                .ipAddress(ip)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
