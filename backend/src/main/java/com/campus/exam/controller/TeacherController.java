package com.campus.exam.controller;

import com.campus.exam.dto.*;
import com.campus.exam.entity.ExamAttempt;
import com.campus.exam.entity.GradeAppeal;
import com.campus.exam.entity.Notification;
import com.campus.exam.entity.StudentAnswer;
import com.campus.exam.entity.Paper;
import com.campus.exam.entity.Question;
import com.campus.exam.entity.User;
import com.campus.exam.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {
    private final RoleGuard roleGuard;
    private final QuestionService questionService;
    private final PaperService paperService;
    private final ExamService examService;
    private final AiAuthoringService aiAuthoringService;

    @GetMapping("/questions")
    public ApiResponse<List<Question>> questions(@RequestParam(required = false) Long courseId) {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(questionService.list(courseId, user));
    }

    @GetMapping("/questions/{id}")
    public ApiResponse<QuestionDto> question(@PathVariable Long id) {
        roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(questionService.detail(id));
    }

    @PostMapping("/questions")
    public ApiResponse<QuestionDto> saveQuestion(@RequestBody QuestionDto dto) {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(questionService.save(dto, user.getId()));
    }

    @DeleteMapping("/questions/{id}")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long id) {
        roleGuard.require("TEACHER", "ADMIN");
        questionService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/ai/questions")
    public ApiResponse<List<QuestionDto>> aiQuestions(@RequestBody AiQuestionGenerateRequest request) {
        roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(aiAuthoringService.generateQuestions(request));
    }

    @PostMapping("/ai/paper")
    public ApiResponse<AiPaperDraft> aiPaper(@RequestBody AiPaperGenerateRequest request) {
        roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(aiAuthoringService.generatePaper(request));
    }

    @GetMapping("/papers")
    public ApiResponse<List<Paper>> papers() {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(paperService.list(null, user));
    }

    @GetMapping("/papers/{id}/questions")
    public ApiResponse<?> paperQuestions(@PathVariable Long id) {
        roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(paperService.questions(id));
    }

    @PostMapping("/papers")
    public ApiResponse<PaperDto> savePaper(@RequestBody PaperDto dto) {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(paperService.save(dto, user.getId()));
    }

    @DeleteMapping("/papers/{id}")
    public ApiResponse<Void> deletePaper(@PathVariable Long id) {
        roleGuard.require("TEACHER", "ADMIN");
        paperService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/scores")
    public ApiResponse<?> scores() {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(examService.scoreRows(user));
    }

    @PostMapping("/questions/import")
    public ApiResponse<Map<String, Object>> importQuestions(@RequestParam("file") MultipartFile file) {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(questionService.importExcel(file, user.getId()));
    }

    @GetMapping("/questions/duplicates")
    public ApiResponse<List<Map<String, Object>>> duplicates() {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(questionService.duplicates(user));
    }

    @PostMapping("/questions/{id}/review")
    public ApiResponse<Void> reviewQuestion(@PathVariable Long id, @RequestBody Map<String, String> body) {
        roleGuard.require("TEACHER", "ADMIN");
        questionService.review(id, body == null ? null : body.get("status"));
        return ApiResponse.ok(null);
    }

    @GetMapping("/manual")
    public ApiResponse<List<Map<String, Object>>> manual() {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(examService.pendingManualAnswers(user));
    }

    @PostMapping("/manual/{answerId}")
    public ApiResponse<StudentAnswer> manualGrade(@PathVariable Long answerId, @RequestBody ManualGradeRequest request) {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(examService.manualGrade(answerId, request, user.getId()));
    }

    @GetMapping("/analysis")
    public ApiResponse<List<Map<String, Object>>> analysis() {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(examService.analysisReport(user));
    }

    @GetMapping("/monitor")
    public ApiResponse<List<Map<String, Object>>> monitor() {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(examService.monitorEvents(user));
    }

    @GetMapping("/appeals")
    public ApiResponse<List<GradeAppeal>> appeals() {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(examService.appeals(user));
    }

    @PostMapping("/appeals/{id}")
    public ApiResponse<GradeAppeal> reviewAppeal(@PathVariable Long id, @RequestBody AppealReviewRequest request) {
        User user = roleGuard.require("TEACHER", "ADMIN");
        return ApiResponse.ok(examService.reviewAppeal(id, request, user.getId()));
    }
}
