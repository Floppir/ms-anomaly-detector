package com.example.service;

import com.example.model.entity.AnomalyAlert;
import com.example.model.enums.AnomalyType;
import com.example.model.event.EventType;
import com.example.model.event.UserEventMessage;
import com.example.repository.AnomalyAlertRepository;
import com.example.repository.UserActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UserActivityServiceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private UserActivityService service;

    @Autowired
    private UserActivityRepository activityRepository;

    @Autowired
    private AnomalyAlertRepository alertRepository;

    @BeforeEach
    void cleanUp() {
        alertRepository.deleteAll();
        activityRepository.deleteAll();
    }

    @Test
    void processEvent_savesActivityWithParsedLoginPayload() {
        var event = loginEvent(1L, "1.2.3.4", "RU", "Moscow", "fp-abc", LocalDateTime.now());

        service.processEvent(event);

        var activities = activityRepository.findAll();
        assertThat(activities).hasSize(1);
        var saved = activities.getFirst();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getCountry()).isEqualTo("RU");
        assertThat(saved.getCity()).isEqualTo("Moscow");
        assertThat(saved.getDeviceFingerprint()).isEqualTo("fp-abc");
    }

    @Test
    void processEvent_savesActivityWhenPayloadIsNull() {
        var event = UserEventMessage.builder()
                .userId(3L)
                .eventType(EventType.FAILED_LOGIN)
                .ipAddress("2.2.2.2")
                .createdAt(LocalDateTime.now())
                .build();

        service.processEvent(event);

        assertThat(activityRepository.findAll()).hasSize(1);
        assertThat(alertRepository.findAll()).isEmpty();
    }

    @Test
    void processEvent_savesActivityWithNullGeoFields_whenLoginPayloadHasNoExpectedFields() {
        var event = UserEventMessage.builder()
                .userId(7L)
                .eventType(EventType.LOGIN)
                .ipAddress("3.3.3.3")
                .payload("{\"unexpected\":\"value\"}")
                .createdAt(LocalDateTime.now())
                .build();

        service.processEvent(event);

        var activities = activityRepository.findAll();
        assertThat(activities).hasSize(1);
        assertThat(activities.getFirst().getCountry()).isNull();
        assertThat(activities.getFirst().getCity()).isNull();
    }

    @Test
    void processEvent_detectsBruteForce_whenFailedLoginThresholdExceeded() {
        long userId = 42L;
        String ip = "5.5.5.5";

        for (int i = 1; i <= 5; i++) {
            service.processEvent(failedLoginEvent(userId, ip, LocalDateTime.now().minusMinutes(10 - i)));
        }

        service.processEvent(failedLoginEvent(userId, ip, LocalDateTime.now()));

        List<AnomalyAlert> alerts = alertRepository.findByUserIdAndAnomalyType(userId, AnomalyType.BRUTE_FORCE);
        assertThat(alerts).isNotEmpty();
        assertThat(alerts.getFirst().getUserId()).isEqualTo(userId);
    }

    @Test
    void processEvent_doesNotDetectBruteForce_whenBelowThreshold() {
        long userId = 10L;

        for (int i = 0; i < 4; i++) {
            service.processEvent(failedLoginEvent(userId, "1.2.3.4", LocalDateTime.now().minusMinutes(i + 1)));
        }

        assertThat(alertRepository.findByUserIdAndAnomalyType(userId, AnomalyType.BRUTE_FORCE)).isEmpty();
    }

    @Test
    void processEvent_detectsImpossibleTravel_whenDifferentCountriesWithinWindow() {
        long userId = 20L;

        service.processEvent(loginEvent(userId, "1.1.1.1", "RU", "Moscow", "fp1",
                LocalDateTime.now().minusMinutes(30)));

        service.processEvent(loginEvent(userId, "2.2.2.2", "US", "New York", "fp2",
                LocalDateTime.now()));

        List<AnomalyAlert> alerts =
                alertRepository.findByUserIdAndAnomalyType(userId, AnomalyType.IMPOSSIBLE_TRAVEL);
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().getSeverity().name()).isEqualTo("CRITICAL");
    }

    @Test
    void processEvent_doesNotDetectImpossibleTravel_whenSameCountry() {
        long userId = 21L;

        service.processEvent(loginEvent(userId, "1.1.1.1", "RU", "Moscow",     "fp1", LocalDateTime.now().minusMinutes(30)));
        service.processEvent(loginEvent(userId, "1.1.1.2", "RU", "Saint-Pete", "fp2", LocalDateTime.now()));

        assertThat(alertRepository.findByUserIdAndAnomalyType(userId, AnomalyType.IMPOSSIBLE_TRAVEL)).isEmpty();
    }

    @Test
    void processEvent_doesNotDetectImpossibleTravel_onFirstLogin() {
        long userId = 22L;

        service.processEvent(loginEvent(userId, "1.1.1.1", "DE", "Berlin", "fp1", LocalDateTime.now()));

        assertThat(alertRepository.findByUserIdAndAnomalyType(userId, AnomalyType.IMPOSSIBLE_TRAVEL)).isEmpty();
    }

    @Test
    void processEvent_savesActivity_whenAllDetectorsSkipEvent() {
        var event = UserEventMessage.builder()
                .userId(99L)
                .eventType(EventType.REGISTER)
                .ipAddress("0.0.0.0")
                .createdAt(LocalDateTime.now())
                .build();

        service.processEvent(event);

        assertThat(activityRepository.findAll()).hasSize(1);
    }

    private UserEventMessage loginEvent(Long userId, String ip, String country, String city,
                                        String fingerprint, LocalDateTime at) {
        String payload = String.format(
                "{\"country\":\"%s\",\"city\":\"%s\",\"deviceFingerprint\":\"%s\"}",
                country, city, fingerprint);
        return UserEventMessage.builder()
                .userId(userId)
                .eventType(EventType.LOGIN)
                .ipAddress(ip)
                .payload(payload)
                .createdAt(at)
                .build();
    }

    private UserEventMessage failedLoginEvent(Long userId, String ip, LocalDateTime at) {
        return UserEventMessage.builder()
                .userId(userId)
                .eventType(EventType.FAILED_LOGIN)
                .ipAddress(ip)
                .createdAt(at)
                .build();
    }
}
