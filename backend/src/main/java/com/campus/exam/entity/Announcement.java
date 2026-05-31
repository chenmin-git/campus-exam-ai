package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement {
    private Long id;
    private String title;
    private String content;
    private Long creatorId;
    private Integer enabled;
    private LocalDateTime createdAt;
}
