package com.example.listener;

import com.example.model.event.UserEventMessage;
import com.example.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final UserActivityService userActivityService;

    @KafkaListener(topics = "${kafka.topic.user-activity-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void onUserEvent(UserEventMessage event) {
        log.debug("Received event: type={}, userId={}", event.getEventType(), event.getUserId());
        userActivityService.processEvent(event);
    }
}