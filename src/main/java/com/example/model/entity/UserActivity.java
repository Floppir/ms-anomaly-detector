package com.example.model.entity;

import com.example.model.event.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities", indexes = {
        @Index(name = "idx_user_activities_user_id_event_type", columnList = "userId, eventType"),
        @Index(name = "idx_user_activities_ip_address", columnList = "ipAddress"),
        @Index(name = "idx_user_activities_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_activities_seq")
    @SequenceGenerator(name = "user_activities_seq", sequenceName = "user_activities_id_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType eventType;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}