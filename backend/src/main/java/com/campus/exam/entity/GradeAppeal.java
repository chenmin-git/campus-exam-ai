package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("grade_appeal")
public class GradeAppeal {
    private Long id;
    private Long attemptId;
    private Long studentId;
    private String reason;
    private String status;
    private String reply;
    private Long reviewerId;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
