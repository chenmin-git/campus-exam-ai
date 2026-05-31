package com.campus.exam.controller;

import com.campus.exam.dto.ApiResponse;
import com.campus.exam.dto.AppealRequest;
import com.campus.exam.dto.MonitorEventRequest;
import com.campus.exam.dto.PasswordRequest;
import com.campus.exam.dto.SubmitExamRequest;
import com.campus.exam.entity.GradeAppeal;
import com.campus.exam.entity.Notification;
import com.campus.exam.entity.ExamAttempt;
import com.campus.exam.entity.User;
import com.campus.exam.mapper.UserMapper;
import com.campus.exam.service.AiAnalysisService;
import com.campus.exam.service.ExamService;
import com.campus.exam.service.NotificationService;
import com.campus.exam.service.RoleGuard;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {
    private final RoleGuard roleGuard;
    private final ExamService examService;
    private final AiAnalysisService aiAnalysisService;
    private final NotificationService notificationService;
    private final com.campus.exam.service.AuthService authService;
    private final UserMapper userMapper;

    @GetMapping("/exams")
    public ApiResponse<?> exams() {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(examService.availablePapers(user));
    }

    @PostMapping("/exams/{id}/start")
    public ApiResponse<Map<String, Object>> start(@PathVariable Long id) {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(examService.start(id, user.getId()));
    }

    @PostMapping("/attempts/{id}/submit")
    public ApiResponse<ExamAttempt> submit(@PathVariable Long id, @RequestBody SubmitExamRequest request) {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(examService.submit(id, request, user.getId()));
    }

    @GetMapping("/attempts")
    public ApiResponse<List<ExamAttempt>> attempts() {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(examService.attempts(user.getId()));
    }

    @GetMapping("/wrong-questions")
    public ApiResponse<List<Map<String, Object>>> wrongQuestions() {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(examService.wrongQuestions(user.getId()));
    }

    @GetMapping("/study-advice")
    public ApiResponse<List<Map<String, Object>>> studyAdvice() {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(examService.studyAdvice(user.getId()));
    }

    @GetMapping("/appeals")
    public ApiResponse<List<GradeAppeal>> appeals() {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(examService.appeals(user));
    }

    @PostMapping("/appeals")
    public ApiResponse<GradeAppeal> createAppeal(@RequestBody AppealRequest request) {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(examService.createAppeal(request, user.getId()));
    }

    @PostMapping("/wrong-questions/{questionId}/ai-analysis")
    public ApiResponse<String> aiAnalysis(@PathVariable Long questionId, @RequestBody(required = false) Map<String, String> body) {
        roleGuard.require("STUDENT", "ADMIN");
        String studentAnswer = body == null ? "" : body.getOrDefault("studentAnswer", "");
        return ApiResponse.ok(aiAnalysisService.analyze(questionId, studentAnswer));
    }

    @PostMapping("/monitor")
    public ApiResponse<?> monitor(@RequestBody MonitorEventRequest request, HttpServletRequest httpRequest) {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(examService.recordMonitor(request, httpRequest, user.getId()));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<Notification>> notifications() {
        User user = roleGuard.require("STUDENT", "ADMIN");
        return ApiResponse.ok(notificationService.list(user));
    }

    @PostMapping("/notifications/{id}/read")
    public ApiResponse<Void> readNotification(@PathVariable Long id) {
        User user = roleGuard.require("STUDENT", "ADMIN");
        notificationService.markRead(id, user);
        return ApiResponse.ok(null);
    }

    @GetMapping("/profile")
    public ApiResponse<User> profile() {
        User current = roleGuard.require("STUDENT", "ADMIN");
        User user = userMapper.selectById(current.getId());
        user.setPassword(null);
        return ApiResponse.ok(user);
    }

    @PostMapping("/profile")
    public ApiResponse<User> updateProfile(@RequestBody User input) {
        User current = roleGuard.require("STUDENT", "ADMIN");
        User user = userMapper.selectById(current.getId());
        user.setRealName(input.getRealName());
        user.setPhone(input.getPhone());
        user.setEmail(input.getEmail());
        userMapper.updateById(user);
        user.setPassword(null);
        return ApiResponse.ok(user);
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody PasswordRequest request) {
        User current = roleGuard.require("STUDENT", "ADMIN");
        User user = userMapper.selectById(current.getId());
        if (!authService.matchesPassword(request.oldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        if (request.newPassword() == null || request.newPassword().length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        user.setPassword(authService.encodePassword(request.newPassword()));
        userMapper.updateById(user);
        return ApiResponse.ok(null);
    }
}
