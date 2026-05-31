package com.campus.exam.dto;

public record NotificationRequest(Long userId, String role, String title, String content) {
}
