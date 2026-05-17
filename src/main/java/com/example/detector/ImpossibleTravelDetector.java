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
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImpossibleTravelDetector implements AnomalyDetector {

    private final UserActivityRepository userActivityRepository;

    @Value("${anomaly.impossible-travel.window-minutes}")
    private int windowMinutes;

    @Override
    public List<AnomalyAlert> detect(UserEventMessage event, UserActivity savedActivity) {
        if (event.getEventType() != EventType.LOGIN) {
            return Collections.emptyList();
        }
        var currentCountry = savedActivity.getCountry();
        if (currentCountry == null || currentCountry.isBlank()) {
            return Collections.emptyList();
        }
        Optional<UserActivity> previousLogin = userActivityRepository
                .findTopByUserIdAndEventTypeOrderByCreatedAtDesc(event.getUserId(), EventType.LOGIN);
        if (previousLogin.isEmpty()) {
            return Collections.emptyList();
        }
        var prev = previousLogin.get();
        if (prev.getId().equals(savedActivity.getId())) {
            return Collections.emptyList();
        }
        boolean withinWindow = prev.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(windowMinutes));
        boolean differentCountry = !currentCountry.equalsIgnoreCase(prev.getCountry());

        if (withinWindow && differentCountry && prev.getCountry() != null) {
            String description = String.format(
                    "Impossible travel: user %d logged in from %s, previously logged in from %s within %d minutes",
                    event.getUserId(), currentCountry, prev.getCountry(), windowMinutes);

            return List.of(AnomalyAlert.builder()
                    .userId(event.getUserId())
                    .anomalyType(AnomalyType.IMPOSSIBLE_TRAVEL)
                    .severity(AnomalySeverity.CRITICAL)
                    .description(description)
                    .ipAddress(event.getIpAddress())
                    .detectedAt(LocalDateTime.now())
                    .resolved(false)
                    .build());
        }

        return Collections.emptyList();
    }
}
