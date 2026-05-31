package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("backup_record")
public class BackupRecord {
    private Long id;
    private String name;
    private String filePath;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
}
