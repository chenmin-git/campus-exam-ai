package com.campus.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exam.dto.ApiResponse;
import com.campus.exam.config.AuthContext;
import com.campus.exam.entity.*;
import com.campus.exam.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CommonController {
    private final CourseMapper courseMapper;
    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final QuestionMapper questionMapper;
    private final PaperMapper paperMapper;
    private final ExamAttemptMapper attemptMapper;
    private final StudentAnswerMapper answerMapper;

    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP"));
    }

    @GetMapping("/api/common/courses")
    public ApiResponse<List<Course>> courses() {
        return ApiResponse.ok(courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByAsc(Course::getId)));
    }

    @GetMapping("/api/common/announcements")
    public ApiResponse<List<Announcement>> announcements() {
        return ApiResponse.ok(announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getEnabled, 1)
                .orderByDesc(Announcement::getId)));
    }

    @GetMapping("/api/common/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        User current = AuthContext.user();
        if ("STUDENT".equals(current.getRole())) {
            return ApiResponse.ok(studentDashboard(current));
        }
        if ("TEACHER".equals(current.getRole())) {
            return ApiResponse.ok(teacherDashboard(current));
        }
        return ApiResponse.ok(adminDashboard());
    }

    private Map<String, Object> adminDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", "ADMIN");
        data.put("userCount", userMapper.selectCount(null));
        data.put("studentCount", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT")));
        data.put("teacherCount", userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, "TEACHER")));
        data.put("courseCount", courseMapper.selectCount(null));
        data.put("questionCount", questionMapper.selectCount(null));
        data.put("paperCount", paperMapper.selectCount(null));
        data.put("attemptCount", attemptMapper.selectCount(null));

        List<Question> questions = questionMapper.selectList(null);
        data.put("questionTypes", questions.stream()
                .collect(Collectors.groupingBy(Question::getType, LinkedHashMap::new, Collectors.counting())));

        List<ExamAttempt> attempts = attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>().orderByDesc(ExamAttempt::getId));
        double average = attempts.stream()
                .filter(a -> a.getScore() != null)
                .mapToInt(ExamAttempt::getScore)
                .average()
                .orElse(0);
        data.put("averageScore", Math.round(average * 10.0) / 10.0);
        data.put("recentScores", attempts.stream().limit(8).map(a -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("score", a.getScore());
            row.put("status", a.getStatus());
            row.put("submittedAt", a.getSubmittedAt());
            return row;
        }).toList());

        List<Map<String, Object>> courseQuestions = new ArrayList<>();
        for (Course course : courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByAsc(Course::getId))) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", course.getName());
            row.put("count", questions.stream().filter(q -> course.getId().equals(q.getCourseId())).count());
            courseQuestions.add(row);
        }
        data.put("courseQuestions", courseQuestions);
        return data;
    }

    private Map<String, Object> teacherDashboard(User current) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", "TEACHER");
        List<Question> questions = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCreatorId, current.getId()));
        List<Paper> papers = paperMapper.selectList(new LambdaQueryWrapper<Paper>()
                .eq(Paper::getCreatorId, current.getId())
                .orderByDesc(Paper::getId));
        List<Long> paperIds = papers.stream().map(Paper::getId).toList();
        List<ExamAttempt> attempts = paperIds.isEmpty() ? List.of() : attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>()
                .in(ExamAttempt::getPaperId, paperIds)
                .orderByDesc(ExamAttempt::getId));
        data.put("questionCount", questions.size());
        data.put("paperCount", papers.size());
        data.put("publishedPaperCount", papers.stream().filter(p -> p.getPublished() != null && p.getPublished() == 1).count());
        data.put("attemptCount", attempts.size());
        data.put("averageScore", roundAverage(attempts));
        data.put("questionTypes", questions.stream()
                .collect(Collectors.groupingBy(Question::getType, LinkedHashMap::new, Collectors.counting())));
        data.put("paperStatus", Map.of(
                "已发布", papers.stream().filter(p -> p.getPublished() != null && p.getPublished() == 1).count(),
                "草稿", papers.stream().filter(p -> p.getPublished() == null || p.getPublished() == 0).count()
        ));
        data.put("recentScores", attempts.stream().limit(8).map(a -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("score", a.getScore());
            row.put("status", a.getStatus());
            row.put("submittedAt", a.getSubmittedAt());
            return row;
        }).toList());
        List<Map<String, Object>> courseQuestions = new ArrayList<>();
        for (Course course : courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByAsc(Course::getId))) {
            long count = questions.stream().filter(q -> course.getId().equals(q.getCourseId())).count();
            if (count > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", course.getName());
                row.put("count", count);
                courseQuestions.add(row);
            }
        }
        data.put("courseQuestions", courseQuestions);
        return data;
    }

    private Map<String, Object> studentDashboard(User current) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", "STUDENT");
        List<Paper> papers = paperMapper.selectList(new LambdaQueryWrapper<Paper>().eq(Paper::getPublished, 1));
        List<ExamAttempt> attempts = attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>()
                .eq(ExamAttempt::getStudentId, current.getId())
                .orderByDesc(ExamAttempt::getId));
        List<Long> attemptIds = attempts.stream().map(ExamAttempt::getId).toList();
        long wrongCount = attemptIds.isEmpty() ? 0 : answerMapper.selectCount(new LambdaQueryWrapper<StudentAnswer>()
                .in(StudentAnswer::getAttemptId, attemptIds)
                .eq(StudentAnswer::getCorrect, 0));
        data.put("availableExamCount", papers.size());
        data.put("attemptCount", attempts.size());
        data.put("finishedCount", attempts.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).count());
        data.put("wrongCount", wrongCount);
        data.put("averageScore", roundAverage(attempts));
        data.put("bestScore", attempts.stream().filter(a -> a.getScore() != null).mapToInt(ExamAttempt::getScore).max().orElse(0));
        data.put("scoreTrend", attempts.stream().limit(8).map(a -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("paperId", a.getPaperId());
            row.put("score", a.getScore());
            row.put("submittedAt", a.getSubmittedAt());
            return row;
        }).toList());
        data.put("studyProgress", List.of(
                Map.of("name", "可参加考试", "count", papers.size()),
                Map.of("name", "已完成考试", "count", attempts.stream().filter(a -> "SUBMITTED".equals(a.getStatus())).count()),
                Map.of("name", "待复盘错题", "count", wrongCount)
        ));
        return data;
    }

    private double roundAverage(List<ExamAttempt> attempts) {
        double average = attempts.stream()
                .filter(a -> a.getScore() != null)
                .mapToInt(ExamAttempt::getScore)
                .average()
                .orElse(0);
        return Math.round(average * 10.0) / 10.0;
    }
}
