package com.example.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginPayload {
    private String sessionId;
    private String country;
    private String city;
    private String loginMethod;
    private boolean newDevice;
    private String deviceFingerprint;
}