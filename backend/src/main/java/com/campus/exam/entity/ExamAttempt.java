package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_attempt")
public class ExamAttempt {
    private Long id;
    private Long paperId;
    private Long studentId;
    private Integer score;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private String status;
    private String reviewStatus;
    private String reviewComment;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime dueAt;
}
