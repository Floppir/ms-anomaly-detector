package com.example.service;

import com.example.detector.AnomalyDetector;
import com.example.model.entity.AnomalyAlert;
import com.example.model.entity.UserActivity;
import com.example.model.event.EventType;
import com.example.model.event.LoginPayload;
import com.example.model.event.UserEventMessage;
import com.example.repository.AnomalyAlertRepository;
import com.example.repository.UserActivityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final AnomalyAlertRepository anomalyAlertRepository;
    private final List<AnomalyDetector> detectors;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processEvent(UserEventMessage event) {
        var activity = buildActivity(event);
        detectors.stream()
                .flatMap(detector -> {
                    try {
                        return detector.detect(event, activity).stream();
                    } catch (Exception e) {
                        log.error("Detector {} failed for userId={}: {}",
                                detector.getClass().getSimpleName(), event.getUserId(), e.getMessage());
                        return java.util.stream.Stream.empty();
                    }
                })
                .forEach(alert -> {
                    anomalyAlertRepository.save(alert);
                    log.warn("Anomaly detected: type={}, severity={}, userId={}, ip={}",
                            alert.getAnomalyType(), alert.getSeverity(), alert.getUserId(), alert.getIpAddress());
                });
        userActivityRepository.save(activity);

    }

    private UserActivity buildActivity(UserEventMessage event) {
        UserActivity.UserActivityBuilder builder = UserActivity.builder()
                .userId(event.getUserId())
                .eventType(event.getEventType())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .rawPayload(event.getPayload())
                .createdAt(event.getCreatedAt());

        if (event.getEventType() == EventType.LOGIN && event.getPayload() != null) {
            try {
                LoginPayload login = objectMapper.readValue(event.getPayload(), LoginPayload.class);
                builder.country(login.getCountry())
                        .city(login.getCity())
                        .deviceFingerprint(login.getDeviceFingerprint());
            } catch (Exception e) {
                log.warn("Failed to parse LoginPayload for userId={}: {}", event.getUserId(), e.getMessage());
            }
        }

        return builder.build();
    }
}