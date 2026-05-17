package com.example.detector;

import com.example.model.entity.UserActivity;
import com.example.model.enums.AnomalySeverity;
import com.example.model.enums.AnomalyType;
import com.example.model.event.EventType;
import com.example.model.event.UserEventMessage;
import com.example.repository.UserActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImpossibleTravelDetectorTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @InjectMocks
    private ImpossibleTravelDetector detector;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(detector, "windowMinutes", 60);
    }

    @Test
    void shouldNotTriggerForNonLoginEvent() {
        var event = buildEvent(EventType.FAILED_LOGIN, "Russia");
        var result = detector.detect(event, UserActivity.builder().country("Russia").build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerWhenNoCountry() {
        var event = buildEvent(EventType.LOGIN, null);
        var result = detector.detect(event, UserActivity.builder().build());
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerWhenNoPreviousLogin() {
        var event = buildEvent(EventType.LOGIN, "Russia");
        var activity = UserActivity.builder().id(1L).country("Russia").build();
        when(userActivityRepository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(eq(1L), eq(EventType.LOGIN)))
                .thenReturn(Optional.empty());

        var result = detector.detect(event, activity);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerWhenSameCountry() {
        var event = buildEvent(EventType.LOGIN, "Russia");
        var current = UserActivity.builder().id(2L).country("Russia").build();
        var previous = UserActivity.builder().id(1L).country("Russia")
                .createdAt(LocalDateTime.now().minusMinutes(30)).build();

        when(userActivityRepository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(eq(1L), eq(EventType.LOGIN)))
                .thenReturn(Optional.of(previous));

        var result = detector.detect(event, current);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldTriggerWhenDifferentCountryWithinWindow() {
        var event = buildEvent(EventType.LOGIN, "China");
        var current = UserActivity.builder().id(2L).country("China").build();
        var previous = UserActivity.builder().id(1L).country("Russia")
                .createdAt(LocalDateTime.now().minusMinutes(15)).build();

        when(userActivityRepository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(eq(1L), eq(EventType.LOGIN)))
                .thenReturn(Optional.of(previous));

        var result = detector.detect(event, current);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnomalyType()).isEqualTo(AnomalyType.IMPOSSIBLE_TRAVEL);
        assertThat(result.get(0).getSeverity()).isEqualTo(AnomalySeverity.CRITICAL);
    }

    @Test
    void shouldNotTriggerWhenPreviousLoginOutsideWindow() {
        var event = buildEvent(EventType.LOGIN, "China");
        var current = UserActivity.builder().id(2L).country("China").build();
        var previous = UserActivity.builder().id(1L).country("Russia")
                .createdAt(LocalDateTime.now().minusHours(3)).build();

        when(userActivityRepository.findTopByUserIdAndEventTypeOrderByCreatedAtDesc(eq(1L), eq(EventType.LOGIN)))
                .thenReturn(Optional.of(previous));

        var result = detector.detect(event, current);
        assertThat(result).isEmpty();
    }

    private UserEventMessage buildEvent(EventType type, String country) {
        return UserEventMessage.builder()
                .userId(1L)
                .eventType(type)
                .ipAddress("1.2.3.4")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
