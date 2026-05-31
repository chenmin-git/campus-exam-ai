package com.campus.exam.dto;

public record MonitorEventRequest(Long attemptId, String eventType, String detail) {
}
