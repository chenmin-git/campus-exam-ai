package com.campus.exam.dto;

import java.util.List;

public record AiPaperGenerateRequest(
        Long courseId,
        String title,
        String topic,
        Integer questionCount,
        Integer totalScore,
        Integer durationMinutes,
        Integer singleCount,
        Integer multipleCount,
        Integer judgeCount,
        Integer shortCount,
        Integer programCount,
        Integer easyCount,
        Integer mediumCount,
        Integer hardCount,
        List<String> knowledgePoints
) {
}
