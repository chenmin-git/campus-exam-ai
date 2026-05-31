package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String role;
    private Long classId;
    private String phone;
    private String email;
    private Integer enabled;
    private LocalDateTime createdAt;
}
