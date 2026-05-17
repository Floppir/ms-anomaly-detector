package com.example.detector;

import com.example.model.entity.AnomalyAlert;
import com.example.model.entity.UserActivity;
import com.example.model.event.UserEventMessage;

import java.util.List;

public interface AnomalyDetector {
    List<AnomalyAlert> detect(UserEventMessage event, UserActivity savedActivity);
}
