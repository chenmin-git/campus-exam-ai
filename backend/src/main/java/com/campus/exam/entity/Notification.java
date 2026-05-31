package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    private Long id;
    private Long userId;
    private String role;
    private String title;
    private String content;
    private Integer readFlag;
    private LocalDateTime createdAt;
}
