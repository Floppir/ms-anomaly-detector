package com.example.repository;

import com.example.model.entity.AnomalyAlert;
import com.example.model.enums.AnomalyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlert, Long> {

    List<AnomalyAlert> findAllByOrderByDetectedAtDesc();

    List<AnomalyAlert> findByUserIdOrderByDetectedAtDesc(Long userId);

    List<AnomalyAlert> findByResolvedFalseOrderByDetectedAtDesc();

    List<AnomalyAlert> findByUserIdAndAnomalyType(Long userId, AnomalyType anomalyType);
}
