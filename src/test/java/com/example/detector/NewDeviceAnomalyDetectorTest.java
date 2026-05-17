package com.example.detector;

import com.example.model.entity.UserActivity;
import com.example.model.enums.AnomalyType;
import com.example.model.event.EventType;
import com.example.model.event.UserEventMessage;
import com.example.repository.UserActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class NewDeviceAnomalyDetectorTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @InjectMocks
    private NewDeviceAnomalyDetector detector;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(detector, "windowHours", 24);
        ReflectionTestUtils.setField(detector, "objectMapper", objectMapper);
    }

    @Test
    void shouldNotTriggerForNonLoginEvent() throws Exception {
        var event = buildEvent(EventType.FAILED_LOGIN, true, "fp1");
        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerForKnownDevice() throws Exception {
        var event = buildEvent(EventType.LOGIN, false, "fp1");
        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerWhenNoRecentFailures() throws Exception {
        var event = buildEvent(EventType.LOGIN, true, "fp-new");
        when(userActivityRepository.findByUserIdAndEventTypeAndCreatedAtAfter(eq(1L), eq(EventType.FAILED_LOGIN), any()))
                .thenReturn(Collections.emptyList());

        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldTriggerWhenNewDeviceAfterFailures() throws Exception {
        var event = buildEvent(EventType.LOGIN, true, "fp-attacker");
        var failures = List.of(
                UserActivity.builder().build(),
                UserActivity.builder().build()
        );
        when(userActivityRepository.findByUserIdAndEventTypeAndCreatedAtAfter(eq(1L), eq(EventType.FAILED_LOGIN), any()))
                .thenReturn(failures);

        var result = detector.detect(event, UserActivity.builder().build());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnomalyType()).isEqualTo(AnomalyType.NEW_DEVICE_AFTER_FAILED_LOGINS);
        assertThat(result.get(0).getDescription()).contains("fp-attacker");
        assertThat(result.get(0).getDescription()).contains("2");
    }

    @Test
    void shouldHandleInvalidPayloadGracefully() {
        var event = UserEventMessage.builder()
                .userId(1L)
                .eventType(EventType.LOGIN)
                .ipAddress("1.2.3.4")
                .payload("not-valid-json")
                .createdAt(LocalDateTime.now())
                .build();

        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    private UserEventMessage buildEvent(EventType type, boolean newDevice, String fingerprint) throws Exception {
        var payload = objectMapper.writeValueAsString(
                com.example.model.event.LoginPayload.builder()
                        .newDevice(newDevice)
                        .deviceFingerprint(fingerprint)
                        .country("Russia")
                        .city("Moscow")
                        .build());
        return UserEventMessage.builder()
                .userId(1L)
                .eventType(type)
                .ipAddress("1.2.3.4")
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
