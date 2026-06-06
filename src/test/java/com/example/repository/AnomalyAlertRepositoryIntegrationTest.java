package com.example.repository;

import com.example.model.entity.AnomalyAlert;
import com.example.model.enums.AnomalySeverity;
import com.example.model.enums.AnomalyType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class AnomalyAlertRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private AnomalyAlertRepository repository;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 1, 12, 0);

    @Test
    void findAllByOrderByDetectedAtDesc_returnsSortedDescending() {
        saveAlert(1L, AnomalyType.BRUTE_FORCE,       NOW.minusHours(3), false);
        saveAlert(2L, AnomalyType.IMPOSSIBLE_TRAVEL, NOW.minusHours(2), false);
        saveAlert(1L, AnomalyType.SUSPICIOUS_IP,     NOW.minusHours(1), false);

        List<AnomalyAlert> result = repository.findAllByOrderByDetectedAtDesc();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDetectedAt()).isAfter(result.get(1).getDetectedAt());
        assertThat(result.get(1).getDetectedAt()).isAfter(result.get(2).getDetectedAt());
    }

    @Test
    void findByUserIdOrderByDetectedAtDesc_returnsOnlyUserAlerts() {
        saveAlert(1L, AnomalyType.BRUTE_FORCE,   NOW.minusHours(2), false);
        saveAlert(1L, AnomalyType.SUSPICIOUS_IP, NOW.minusHours(1), false);
        saveAlert(2L, AnomalyType.BRUTE_FORCE,   NOW.minusHours(1), false);

        List<AnomalyAlert> result = repository.findByUserIdOrderByDetectedAtDesc(1L);

        assertThat(result)
                .hasSize(2)
                .allMatch(a -> a.getUserId().equals(1L));
        assertThat(result.get(0).getDetectedAt()).isAfter(result.get(1).getDetectedAt());
    }

    @Test
    void findByUserIdOrderByDetectedAtDesc_returnsEmptyWhenNoAlerts() {
        List<AnomalyAlert> result = repository.findByUserIdOrderByDetectedAtDesc(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findByResolvedFalseOrderByDetectedAtDesc_excludesResolvedAlerts() {
        saveAlert(1L, AnomalyType.BRUTE_FORCE,       NOW.minusHours(3), true);
        saveAlert(1L, AnomalyType.SUSPICIOUS_IP,     NOW.minusHours(2), false);
        saveAlert(2L, AnomalyType.IMPOSSIBLE_TRAVEL, NOW.minusHours(1), false);

        List<AnomalyAlert> result = repository.findByResolvedFalseOrderByDetectedAtDesc();

        assertThat(result)
                .hasSize(2)
                .allMatch(a -> !a.isResolved());
        assertThat(result.get(0).getDetectedAt()).isAfter(result.get(1).getDetectedAt());
    }

    @Test
    void findByResolvedFalseOrderByDetectedAtDesc_returnsEmptyWhenAllResolved() {
        saveAlert(1L, AnomalyType.BRUTE_FORCE, NOW.minusHours(1), true);
        saveAlert(2L, AnomalyType.BRUTE_FORCE, NOW.minusHours(2), true);

        List<AnomalyAlert> result = repository.findByResolvedFalseOrderByDetectedAtDesc();

        assertThat(result).isEmpty();
    }

    @Test
    void findByUserIdAndAnomalyType_filtersByUserAndType() {
        saveAlert(1L, AnomalyType.BRUTE_FORCE,   NOW.minusHours(2), false);
        saveAlert(1L, AnomalyType.SUSPICIOUS_IP, NOW.minusHours(1), false);
        saveAlert(2L, AnomalyType.BRUTE_FORCE,   NOW.minusHours(1), false);

        List<AnomalyAlert> result = repository.findByUserIdAndAnomalyType(1L, AnomalyType.BRUTE_FORCE);

        assertThat(result)
                .hasSize(1)
                .allMatch(a -> a.getUserId().equals(1L))
                .allMatch(a -> a.getAnomalyType() == AnomalyType.BRUTE_FORCE);
    }

    @Test
    void save_assignsIdAndPersistsAllFields() {
        AnomalyAlert alert = AnomalyAlert.builder()
                .userId(1L)
                .anomalyType(AnomalyType.BRUTE_FORCE)
                .severity(AnomalySeverity.HIGH)
                .description("Brute force attack detected")
                .ipAddress("1.2.3.4")
                .detectedAt(NOW)
                .resolved(false)
                .build();

        AnomalyAlert saved = repository.save(alert);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDescription()).isEqualTo("Brute force attack detected");
        assertThat(saved.isResolved()).isFalse();
    }

    private AnomalyAlert saveAlert(Long userId, AnomalyType type, LocalDateTime detectedAt, boolean resolved) {
        return repository.save(AnomalyAlert.builder()
                .userId(userId)
                .anomalyType(type)
                .severity(AnomalySeverity.HIGH)
                .detectedAt(detectedAt)
                .resolved(resolved)
                .build());
    }
}
