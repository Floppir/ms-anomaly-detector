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
public class SuspiciousIpDetector implements AnomalyDetector {

    private final UserActivityRepository userActivityRepository;

    @Value("${anomaly.suspicious-ip.max-users}")
    private int maxUsers;

    @Value("${anomaly.suspicious-ip.window-minutes}")
    private int windowMinutes;

    @Override
    public List<AnomalyAlert> detect(UserEventMessage event, UserActivity savedActivity) {
        if (event.getEventType() != EventType.LOGIN && event.getEventType() != EventType.FAILED_LOGIN) {
            return Collections.emptyList();
        }

        String ip = event.getIpAddress();
        if (ip == null || ip.isBlank()) {
            return Collections.emptyList();
        }

        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(windowMinutes);
        long distinctUsers = userActivityRepository
                .countDistinctUsersByIpAddressAndCreatedAtAfter(ip, windowStart);

        if (distinctUsers >= maxUsers) {
            String description = String.format(
                    "Suspicious IP: %d different users accessed from IP %s in the last %d minutes",
                    distinctUsers, ip, windowMinutes);

            return List.of(AnomalyAlert.builder()
                    .userId(event.getUserId())
                    .anomalyType(AnomalyType.SUSPICIOUS_IP)
                    .severity(AnomalySeverity.MEDIUM)
                    .description(description)
                    .ipAddress(ip)
                    .detectedAt(LocalDateTime.now())
                    .resolved(false)
                    .build());
        }

        return Collections.emptyList();
    }
}
