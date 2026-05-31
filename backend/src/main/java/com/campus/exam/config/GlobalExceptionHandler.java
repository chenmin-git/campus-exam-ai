package com.campus.exam.config;

import com.campus.exam.dto.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handle(Exception ex) {
        return ApiResponse.fail(ex.getMessage() == null ? "系统异常" : ex.getMessage());
    }
}
