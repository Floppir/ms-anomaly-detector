package com.example.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedLoginPayload {
    private String reason;
    private int attemptNumber;
    private String lastSuccessfulLoginIp;
}