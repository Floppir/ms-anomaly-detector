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

/**
 * Статистический детектор: сравнивает количество логинов за текущий час
 * со средним историческим значением (метод скользящего среднего).
 * Срабатывает если current_hour_count > avg_hourly * multiplier.
 */
@Component
@RequiredArgsConstructor
public class UnusualLoginFrequencyDetector implements AnomalyDetector {

    private final UserActivityRepository userActivityRepository;

    @Value("${anomaly.unusual-login-frequency.window-days}")
    private int windowDays;

    @Value("${anomaly.unusual-login-frequency.multiplier}")
    private double multiplier;

    @Value("${anomaly.unusual-login-frequency.min-history-logins}")
    private int minHistoryLogins;

    @Override
    public List<AnomalyAlert> detect(UserEventMessage event, UserActivity savedActivity) {
        if (event.getEventType() != EventType.LOGIN) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();

        long currentHourCount = userActivityRepository.countByUserIdAndEventTypeAndCreatedAtAfter(
                event.getUserId(), EventType.LOGIN, now.minusHours(1));

        long historicalTotal = userActivityRepository.countByUserIdAndEventTypeAndCreatedAtAfter(
                event.getUserId(), EventType.LOGIN, now.minusDays(windowDays));

        if (historicalTotal < minHistoryLogins) {
            return Collections.emptyList();
        }

        double avgHourlyLogins = (double) historicalTotal / (windowDays * 24.0);

        if (avgHourlyLogins > 0 && currentHourCount > avgHourlyLogins * multiplier) {
            String description = String.format(
                    "Unusual login frequency for user %d: %d logins in the last hour " +
                    "(%.2f average per hour over last %d days, threshold: %.1fx)",
                    event.getUserId(), currentHourCount, avgHourlyLogins, windowDays, multiplier);

            return List.of(AnomalyAlert.builder()
                    .userId(event.getUserId())
                    .anomalyType(AnomalyType.UNUSUAL_LOGIN_FREQUENCY)
                    .severity(AnomalySeverity.MEDIUM)
                    .description(description)
                    .ipAddress(event.getIpAddress())
                    .detectedAt(now)
                    .resolved(false)
                    .build());
        }

        return Collections.emptyList();
    }
}