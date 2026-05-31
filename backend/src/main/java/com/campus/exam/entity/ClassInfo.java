package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("class_info")
public class ClassInfo {
    private Long id;
    private String name;
    private String grade;
    private String major;
}
