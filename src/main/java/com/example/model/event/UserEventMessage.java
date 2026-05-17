package com.example.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEventMessage {
    private Long userId;
    private EventType eventType;
    private String ipAddress;
    private String userAgent;
    private String payload;
    private LocalDateTime createdAt;
}