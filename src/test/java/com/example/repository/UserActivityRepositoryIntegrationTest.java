package com.example.repository;

import com.example.model.entity.UserActivity;
import com.example.model.event.EventType;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class UserActivityRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private UserActivityRepository repository;

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 6, 1, 12, 0);

    @Test
    void findByUserIdAndEventTypeAndCreatedAtAfter_returnsOnlyMatchingRecords() {
        saveActivity(1L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(5));
        saveActivity(1L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(15));
        saveActivity(2L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(3));
        saveActivity(1L, EventType.LOGIN,        "1.2.3.4", BASE.minusMinutes(2));

        List<UserActivity> result = repository.findByUserIdAndEventTypeAndCreatedAtAfter(
                1L, EventType.FAILED_LOGIN, BASE.minusMinutes(10));

        assertThat(result)
                .hasSize(1)
                .allMatch(a -> a.getUserId().equals(1L))
                .allMatch(a -> a.getEventType() == EventType.FAILED_LOGIN);
    }

    @Test
    void findByIpAddressAndEventTypeAndCreatedAtAfter_filtersByIpAndTime() {
        saveActivity(1L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(5));
        saveActivity(2L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(3));
        saveActivity(3L, EventType.FAILED_LOGIN, "9.9.9.9", BASE.minusMinutes(2));
        saveActivity(1L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(65));

        List<UserActivity> result = repository.findByIpAddressAndEventTypeAndCreatedAtAfter(
                "1.2.3.4", EventType.FAILED_LOGIN, BASE.minusHours(1));

        assertThat(result)
                .hasSize(2)
                .allMatch(a -> a.getIpAddress().equals("1.2.3.4"));
    }

    @Test
    void findTopByUserIdAndEventTypeOrderByCreatedAtDesc_returnsMostRecentEvent() {
        saveActivity(1L, EventType.LOGIN, "1.2.3.4", BASE.minusHours(3));
        saveActivity(1L, EventType.LOGIN, "1.2.3.4", BASE.minusHours(1));
        saveActivity(1L, EventType.LOGIN, "1.2.3.4", BASE.minusHours(2));

        Optional<UserActivity> result =
                repository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(1L, EventType.LOGIN);

        assertThat(result)
                .isPresent()
                .hasValueSatisfying(a -> assertThat(a.getCreatedAt()).isEqualTo(BASE.minusHours(1)));
    }

    @Test
    void findTopByUserIdAndEventTypeOrderByCreatedAtDesc_returnsEmptyWhenNoRecords() {
        Optional<UserActivity> result =
                repository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(99L, EventType.LOGIN);

        assertThat(result).isEmpty();
    }

    @Test
    void countDistinctUsersByIpAddressAndCreatedAtAfter_countsUniqueUsersOnly() {
        saveActivity(1L, EventType.LOGIN, "1.2.3.4", BASE.minusMinutes(10));
        saveActivity(1L, EventType.LOGIN, "1.2.3.4", BASE.minusMinutes(5));
        saveActivity(2L, EventType.LOGIN, "1.2.3.4", BASE.minusMinutes(3));
        saveActivity(3L, EventType.LOGIN, "9.9.9.9", BASE.minusMinutes(2));

        long count = repository.countDistinctUsersByIpAddressAndCreatedAtAfter(
                "1.2.3.4", BASE.minusHours(1));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countByUserIdAndEventTypeAndCreatedAtAfter_countsOnlyMatchingRecords() {
        saveActivity(1L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(3));
        saveActivity(1L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(5));
        saveActivity(1L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(15));
        saveActivity(2L, EventType.FAILED_LOGIN, "1.2.3.4", BASE.minusMinutes(2));

        long count = repository.countByUserIdAndEventTypeAndCreatedAtAfter(
                1L, EventType.FAILED_LOGIN, BASE.minusMinutes(10));

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countDistinctUsersByIpAddressAndCreatedAtAfter_returnsZeroWhenNoRecords() {
        long count = repository.countDistinctUsersByIpAddressAndCreatedAtAfter(
                "0.0.0.0", BASE.minusHours(1));

        assertThat(count).isZero();
    }

    private void saveActivity(Long userId, EventType eventType, String ip, LocalDateTime createdAt) {
        repository.save(UserActivity.builder()
                .userId(userId)
                .eventType(eventType)
                .ipAddress(ip)
                .createdAt(createdAt)
                .build());
    }
}
