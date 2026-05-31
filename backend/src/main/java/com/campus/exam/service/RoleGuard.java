package com.campus.exam.service;

import com.campus.exam.config.AuthContext;
import com.campus.exam.entity.User;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class RoleGuard {
    public User require(String... roles) {
        User user = AuthContext.user();
        if (Arrays.stream(roles).noneMatch(role -> role.equals(user.getRole()))) {
            throw new IllegalArgumentException("无权访问该功能");
        }
        return user;
    }
}
