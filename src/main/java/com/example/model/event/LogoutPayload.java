package com.example.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutPayload {
    private String sessionId;
    private long sessionDurationSeconds;
    private int pagesVisited;
}