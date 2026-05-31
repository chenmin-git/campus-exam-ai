package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_analysis_cache")
public class AiAnalysisCache {
    private Long id;
    private Long questionId;
    private String studentAnswer;
    private String content;
    private LocalDateTime createdAt;
}
