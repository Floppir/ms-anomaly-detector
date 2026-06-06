package com.example.service;

import com.example.detector.AnomalyDetector;
import com.example.model.entity.AnomalyAlert;
import com.example.model.entity.UserActivity;
import com.example.model.enums.AnomalySeverity;
import com.example.model.enums.AnomalyType;
import com.example.model.event.EventType;
import com.example.model.event.UserEventMessage;
import com.example.repository.AnomalyAlertRepository;
import com.example.repository.UserActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @Mock
    private AnomalyAlertRepository anomalyAlertRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private UserActivityService service;

    @Test
    void processEvent_alwaysSavesActivity() {
        service = new UserActivityService(
                userActivityRepository, anomalyAlertRepository, List.of(), objectMapper);

        service.processEvent(failedLoginEvent(1L, "1.2.3.4"));

        verify(userActivityRepository).save(any(UserActivity.class));
    }

    @Test
    void processEvent_parsesLoginPayloadAndPopulatesActivityFields() {
        String payload = "{\"country\":\"RU\",\"city\":\"Moscow\",\"deviceFingerprint\":\"fp-1\"}";
        var event = UserEventMessage.builder()
                .userId(1L)
                .eventType(EventType.LOGIN)
                .ipAddress("1.2.3.4")
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();

        service = new UserActivityService(
                userActivityRepository, anomalyAlertRepository, List.of(), objectMapper);

        var captor = ArgumentCaptor.forClass(UserActivity.class);
        service.processEvent(event);

        verify(userActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getCountry()).isEqualTo("RU");
        assertThat(captor.getValue().getCity()).isEqualTo("Moscow");
        assertThat(captor.getValue().getDeviceFingerprint()).isEqualTo("fp-1");
    }

    @Test
    void processEvent_doesNotParsePayload_forNonLoginEvents() {
        var event = UserEventMessage.builder()
                .userId(1L)
                .eventType(EventType.FAILED_LOGIN)
                .ipAddress("1.2.3.4")
                .payload("{\"country\":\"RU\"}")
                .createdAt(LocalDateTime.now())
                .build();

        service = new UserActivityService(
                userActivityRepository, anomalyAlertRepository, List.of(), objectMapper);

        var captor = ArgumentCaptor.forClass(UserActivity.class);
        service.processEvent(event);

        verify(userActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getCountry()).isNull();
        assertThat(captor.getValue().getCity()).isNull();
    }

    @Test
    void processEvent_invokesAllDetectorsAndSavesAlerts() {
        var alert1 = buildAlert(AnomalyType.BRUTE_FORCE);
        var alert2 = buildAlert(AnomalyType.SUSPICIOUS_IP);

        AnomalyDetector detector1 = (event, activity) -> List.of(alert1);
        AnomalyDetector detector2 = (event, activity) -> List.of(alert2);

        service = new UserActivityService(
                userActivityRepository, anomalyAlertRepository,
                List.of(detector1, detector2), objectMapper);

        service.processEvent(failedLoginEvent(1L, "1.2.3.4"));

        verify(anomalyAlertRepository).save(alert1);
        verify(anomalyAlertRepository).save(alert2);
        verify(userActivityRepository).save(any(UserActivity.class));
    }

    @Test
    void processEvent_continuesAndSavesActivity_whenDetectorThrowsException() {
        var goodAlert = buildAlert(AnomalyType.BRUTE_FORCE);

        AnomalyDetector failingDetector = (event, activity) -> {
            throw new RuntimeException("detector failed");
        };
        AnomalyDetector workingDetector = (event, activity) -> List.of(goodAlert);

        service = new UserActivityService(
                userActivityRepository, anomalyAlertRepository,
                List.of(failingDetector, workingDetector), objectMapper);

        service.processEvent(failedLoginEvent(1L, "1.2.3.4"));

        verify(anomalyAlertRepository).save(goodAlert);
        verify(userActivityRepository).save(any(UserActivity.class));
    }

    @Test
    void processEvent_doesNotSaveAlerts_whenDetectorReturnsEmptyList() {
        AnomalyDetector noop = (event, activity) -> List.of();

        service = new UserActivityService(
                userActivityRepository, anomalyAlertRepository,
                List.of(noop), objectMapper);

        service.processEvent(failedLoginEvent(1L, "1.2.3.4"));

        verify(anomalyAlertRepository, never()).save(any());
        verify(userActivityRepository).save(any(UserActivity.class));
    }

    @Test
    void processEvent_savesActivityWithNullFields_whenLoginPayloadIsInvalidJson() {
        var event = UserEventMessage.builder()
                .userId(1L)
                .eventType(EventType.LOGIN)
                .ipAddress("1.2.3.4")
                .payload("invalid JSON")
                .createdAt(LocalDateTime.now())
                .build();

        service = new UserActivityService(
                userActivityRepository, anomalyAlertRepository, List.of(), objectMapper);

        var captor = ArgumentCaptor.forClass(UserActivity.class);
        service.processEvent(event);

        verify(userActivityRepository).save(captor.capture());
        assertThat(captor.getValue().getCountry()).isNull();
    }

    private UserEventMessage failedLoginEvent(Long userId, String ip) {
        return UserEventMessage.builder()
                .userId(userId)
                .eventType(EventType.FAILED_LOGIN)
                .ipAddress(ip)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AnomalyAlert buildAlert(AnomalyType type) {
        return AnomalyAlert.builder()
                .userId(1L)
                .anomalyType(type)
                .severity(AnomalySeverity.HIGH)
                .detectedAt(LocalDateTime.now())
                .build();
    }
}
