package com.campus.exam.dto;

public record AiQuestionGenerateRequest(
        Long courseId,
        String topic,
        String type,
        Integer count,
        Integer difficulty
) {
}
