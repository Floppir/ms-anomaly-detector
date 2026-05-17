package com.example.model.entity;

import com.example.model.enums.AnomalySeverity;
import com.example.model.enums.AnomalyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_alerts", indexes = {
        @Index(name = "idx_anomaly_alerts_user_id", columnList = "userId"),
        @Index(name = "idx_anomaly_alerts_detected_at", columnList = "detectedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private AnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    private AnomalySeverity severity;

    @Column(length = 1024)
    private String description;

    private String ipAddress;
    private LocalDateTime detectedAt;
    private boolean resolved;
}