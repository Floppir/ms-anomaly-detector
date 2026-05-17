package com.example.repository;

import com.example.model.entity.UserActivity;
import com.example.model.event.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    List<UserActivity> findByUserIdAndEventTypeAndCreatedAtAfter(
            Long userId, EventType eventType, LocalDateTime after);

    List<UserActivity> findByIpAddressAndEventTypeAndCreatedAtAfter(
            String ipAddress, EventType eventType, LocalDateTime after);

    Optional<UserActivity> findTopByUserIdAndEventTypeOrderByCreatedAtDesc(
            Long userId, EventType eventType);

    @Query("SELECT COUNT(DISTINCT a.userId) FROM UserActivity a " +
           "WHERE a.ipAddress = :ipAddress AND a.createdAt > :after")
    long countDistinctUsersByIpAddressAndCreatedAtAfter(
            @Param("ipAddress") String ipAddress,
            @Param("after") LocalDateTime after);

    long countByUserIdAndEventTypeAndCreatedAtAfter(
            Long userId, EventType eventType, LocalDateTime after);
}
