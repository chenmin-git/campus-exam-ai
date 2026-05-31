package com.campus.exam.dto;

import com.campus.exam.entity.User;

import java.util.List;

public record LoginResponse(String token, User user, List<String> permissions) {
}
