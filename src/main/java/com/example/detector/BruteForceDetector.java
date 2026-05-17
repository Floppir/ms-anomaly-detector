package com.example.detector;

import com.example.model.entity.AnomalyAlert;
import com.example.model.entity.UserActivity;
import com.example.model.enums.AnomalySeverity;
import com.example.model.enums.AnomalyType;
import com.example.model.event.EventType;
import com.example.model.event.UserEventMessage;
import com.example.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BruteForceDetector implements AnomalyDetector {

    private final UserActivityRepository userActivityRepository;

    @Value("${anomaly.brute-force.max-attempts}")
    private int maxAttempts;

    @Value("${anomaly.brute-force.window-minutes}")
    private int windowMinutes;

    @Override
    public List<AnomalyAlert> detect(UserEventMessage event, UserActivity savedActivity) {
        if (event.getEventType() != EventType.FAILED_LOGIN) {
            return Collections.emptyList();
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(windowMinutes);

        long failedByUser = userActivityRepository
                .findByUserIdAndEventTypeAndCreatedAtAfter(event.getUserId(), EventType.FAILED_LOGIN, windowStart)
                .size();

        long failedByIp = userActivityRepository
                .findByIpAddressAndEventTypeAndCreatedAtAfter(event.getIpAddress(), EventType.FAILED_LOGIN, windowStart)
                .size();

        if (failedByUser >= maxAttempts || failedByIp >= maxAttempts) {
            String description = String.format(
                    "Brute force detected: %d failed logins by user %d, %d by IP %s in the last %d minutes",
                    failedByUser, event.getUserId(), failedByIp, event.getIpAddress(), windowMinutes);

            return List.of(AnomalyAlert.builder()
                    .userId(event.getUserId())
                    .anomalyType(AnomalyType.BRUTE_FORCE)
                    .severity(AnomalySeverity.HIGH)
                    .description(description)
                    .ipAddress(event.getIpAddress())
                    .detectedAt(LocalDateTime.now())
                    .resolved(false)
                    .build());
        }

        return Collections.emptyList();
    }
}
