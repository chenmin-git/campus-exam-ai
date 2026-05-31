package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("permission")
public class Permission {
    private Long id;
    private String code;
    private String name;
    private String module;
    private String path;
    private Integer sortNo;
}
