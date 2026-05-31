package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    private Long id;
    private Long userId;
    private String username;
    private String role;
    private String action;
    private String target;
    private String detail;
    private LocalDateTime createdAt;
}
