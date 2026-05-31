package com.campus.exam.controller;

import com.campus.exam.dto.ApiResponse;
import com.campus.exam.dto.LoginRequest;
import com.campus.exam.dto.LoginResponse;
import com.campus.exam.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
