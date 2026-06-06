package com.example.detector;

import com.example.model.entity.UserActivity;
import com.example.model.event.EventType;
import com.example.model.event.UserEventMessage;
import com.example.repository.UserActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BruteForceDetectorParameterizedTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @InjectMocks
    private BruteForceDetector detector;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(detector, "maxAttempts", 5);
        ReflectionTestUtils.setField(detector, "windowMinutes", 10);
    }

    @ParameterizedTest(name = "userFailures={0}, ipFailures={1} -> alertExpected={2}")
    @CsvSource({
            "0, 0, false",
            "4, 0, false",
            "5, 0, true",
            "6, 0, true",
            "0, 4, false",
            "0, 5, true",
            "0, 9, true",
            "3, 3, false",
            "5, 5, true"
    })
    void detect_variousFailureCombinations(int userFailures, int ipFailures, boolean expectAlert) {
        stubUserRepo(1L, EventType.FAILED_LOGIN, userFailures);
        stubIpRepo("1.2.3.4", EventType.FAILED_LOGIN, ipFailures);

        var result = detector.detect(buildEvent(EventType.FAILED_LOGIN), UserActivity.builder().build());

        assertThat(result.isEmpty()).isEqualTo(!expectAlert);
    }

    @ParameterizedTest(name = "eventType={0} -> skipped")
    @CsvSource({
            "LOGIN",
            "LOGOUT",
            "REGISTER"
    })
    void detect_skipsNonFailedLoginEvents(EventType eventType) {
        var result = detector.detect(buildEvent(eventType), UserActivity.builder().build());

        assertThat(result).isEmpty();
        verifyNoInteractions(userActivityRepository);
    }

    @ParameterizedTest(name = "userFailures={0}, ipFailures={1}")
    @CsvSource({
            "5, 0",
            "0, 5",
            "7, 3"
    })
    void detect_alertContainsCorrectMetadata(int userFailures, int ipFailures) {
        stubUserRepo(1L, EventType.FAILED_LOGIN, userFailures);
        stubIpRepo("1.2.3.4", EventType.FAILED_LOGIN, ipFailures);

        var result = detector.detect(buildEvent(EventType.FAILED_LOGIN), UserActivity.builder().build());

        assertThat(result).hasSize(1);
        var alert = result.getFirst();
        assertThat(alert.getUserId()).isEqualTo(1L);
        assertThat(alert.getIpAddress()).isEqualTo("1.2.3.4");
        assertThat(alert.isResolved()).isFalse();
        assertThat(alert.getDetectedAt()).isNotNull();
    }

    private void stubUserRepo(Long userId, EventType type, int count) {
        when(userActivityRepository.findByUserIdAndEventTypeAndCreatedAtAfter(
                eq(userId), eq(type), any()))
                .thenReturn(buildList(count));
    }

    private void stubIpRepo(String ip, EventType type, int count) {
        when(userActivityRepository.findByIpAddressAndEventTypeAndCreatedAtAfter(
                eq(ip), eq(type), any()))
                .thenReturn(buildList(count));
    }

    private List<UserActivity> buildList(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> UserActivity.builder().build())
                .toList();
    }

    private UserEventMessage buildEvent(EventType type) {
        return UserEventMessage.builder()
                .userId(1L)
                .eventType(type)
                .ipAddress("1.2.3.4")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
