package com.example.detector;

import com.example.model.entity.AnomalyAlert;
import com.example.model.entity.UserActivity;
import com.example.model.enums.AnomalySeverity;
import com.example.model.enums.AnomalyType;
import com.example.model.event.EventType;
import com.example.model.event.LoginPayload;
import com.example.model.event.UserEventMessage;
import com.example.repository.UserActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewDeviceAnomalyDetector implements AnomalyDetector {

    private final UserActivityRepository userActivityRepository;
    private final ObjectMapper objectMapper;

    @Value("${anomaly.new-device.failed-login-window-hours}")
    private int windowHours;

    @Override
    public List<AnomalyAlert> detect(UserEventMessage event, UserActivity savedActivity) {
        if (event.getEventType() != EventType.LOGIN) {
            return Collections.emptyList();
        }

        try {
            LoginPayload payload = objectMapper.readValue(event.getPayload(), LoginPayload.class);
            if (!payload.isNewDevice()) {
                return Collections.emptyList();
            }

            LocalDateTime windowStart = LocalDateTime.now().minusHours(windowHours);
            List<UserActivity> recentFailures = userActivityRepository
                    .findByUserIdAndEventTypeAndCreatedAtAfter(event.getUserId(), EventType.FAILED_LOGIN, windowStart);

            if (!recentFailures.isEmpty()) {
                String description = String.format(
                        "New device login for user %d (fingerprint: %s) after %d failed login attempts in the last %d hours",
                        event.getUserId(), payload.getDeviceFingerprint(), recentFailures.size(), windowHours);

                return List.of(AnomalyAlert.builder()
                        .userId(event.getUserId())
                        .anomalyType(AnomalyType.NEW_DEVICE_AFTER_FAILED_LOGINS)
                        .severity(AnomalySeverity.MEDIUM)
                        .description(description)
                        .ipAddress(event.getIpAddress())
                        .detectedAt(LocalDateTime.now())
                        .resolved(false)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse LoginPayload for userId={}", event.getUserId(), e);
        }

        return Collections.emptyList();
    }
}
