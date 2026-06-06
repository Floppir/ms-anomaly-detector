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
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type")
    private AnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private AnomalySeverity severity;

    @Column(name = "description", length = 1024)
    private String description;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    @Column(name = "resolved")
    private boolean resolved;
}