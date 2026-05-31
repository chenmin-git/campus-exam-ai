package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_monitor_event")
public class ExamMonitorEvent {
    private Long id;
    private Long attemptId;
    private Long paperId;
    private Long studentId;
    private String eventType;
    private String detail;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}
