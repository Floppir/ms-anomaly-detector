package com.example.model.entity;

import com.example.model.event.EventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private String ipAddress;
    private String userAgent;
    private String country;
    private String city;
    private String deviceFingerprint;
    private LocalDateTime createdAt;
}