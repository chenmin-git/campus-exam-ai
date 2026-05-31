package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question {
    private Long id;
    private Long courseId;
    private Long creatorId;
    private String type;
    private String stem;
    private String correctAnswer;
    private String analysis;
    private String knowledgePoint;
    private String reviewStatus;
    private Integer deleted;
    private Integer difficulty;
    private LocalDateTime createdAt;
}
