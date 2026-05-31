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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final TeacherCourseMapper teacherCourseMapper;
    private final ClassInfoMapper classInfoMapper;
    private final GradeAppealMapper gradeAppealMapper;

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
        data.put("classCount", classInfoMapper.selectCount(null));
        data.put("courseCount", courseMapper.selectCount(null));
        data.put("questionCount", questionMapper.selectCount(null));
        data.put("paperCount", paperMapper.selectCount(null));
        data.put("publishedPaperCount", paperMapper.selectCount(new LambdaQueryWrapper<Paper>().eq(Paper::getPublished, 1)));
        data.put("attemptCount", attemptMapper.selectCount(null));
        data.put("pendingReviewCount", attemptMapper.selectCount(new LambdaQueryWrapper<ExamAttempt>().eq(ExamAttempt::getReviewStatus, "PENDING")));

        List<Question> questions = questionMapper.selectList(null);
        data.put("subjectiveQuestionCount", questions.stream().filter(q -> "SHORT".equals(q.getType()) || "PROGRAM".equals(q.getType())).count());
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
        data.put("pendingReviewCount", attempts.stream().filter(a -> "PENDING".equals(a.getReviewStatus())).count());
        data.put("pendingAppealCount", pendingAppealCount(attempts));
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
        data.put("classCoverage", teacherClassCoverage(current.getId()));
        return data;
    }

    private Map<String, Object> studentDashboard(User current) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("role", "STUDENT");
        List<Paper> papers = availablePapers(current);
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
        data.put("pendingReviewCount", attempts.stream().filter(a -> "PENDING".equals(a.getReviewStatus())).count());
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
        data.put("weakKnowledgePoints", weakKnowledgePoints(attemptIds));
        return data;
    }

    private List<Paper> availablePapers(User current) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .eq(Paper::getPublished, 1)
                .and(w -> w.isNull(Paper::getStartTime).or().le(Paper::getStartTime, now))
                .and(w -> w.isNull(Paper::getEndTime).or().ge(Paper::getEndTime, now))
                .orderByDesc(Paper::getId);
        User student = userMapper.selectById(current.getId());
        if (student == null || student.getClassId() == null) {
            return List.of();
        }
        Set<Long> courseIds = teacherCourseMapper.selectList(new LambdaQueryWrapper<TeacherCourse>()
                        .eq(TeacherCourse::getClassId, student.getClassId()))
                .stream()
                .map(TeacherCourse::getCourseId)
                .collect(Collectors.toSet());
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return paperMapper.selectList(wrapper.in(Paper::getCourseId, courseIds));
    }

    private long pendingAppealCount(List<ExamAttempt> attempts) {
        List<Long> attemptIds = attempts.stream().map(ExamAttempt::getId).toList();
        if (attemptIds.isEmpty()) {
            return 0;
        }
        return gradeAppealMapper.selectCount(new LambdaQueryWrapper<GradeAppeal>()
                .in(GradeAppeal::getAttemptId, attemptIds)
                .eq(GradeAppeal::getStatus, "PENDING"));
    }

    private List<Map<String, Object>> teacherClassCoverage(Long teacherId) {
        return teacherCourseMapper.selectList(new LambdaQueryWrapper<TeacherCourse>().eq(TeacherCourse::getTeacherId, teacherId))
                .stream()
                .map(item -> {
                    ClassInfo classInfo = classInfoMapper.selectById(item.getClassId());
                    Course course = courseMapper.selectById(item.getCourseId());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("classId", item.getClassId());
                    row.put("className", classInfo == null ? "未分班" : className(classInfo));
                    row.put("courseName", course == null ? "未知课程" : course.getName());
                    return row;
                })
                .toList();
    }

    private List<Map<String, Object>> weakKnowledgePoints(List<Long> attemptIds) {
        if (attemptIds.isEmpty()) {
            return List.of();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        List<StudentAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>()
                .in(StudentAnswer::getAttemptId, attemptIds)
                .eq(StudentAnswer::getCorrect, 0)
                .ne(StudentAnswer::getReviewStatus, "PENDING"));
        for (StudentAnswer answer : answers) {
            Question question = questionMapper.selectById(answer.getQuestionId());
            String key = question == null || question.getKnowledgePoint() == null || question.getKnowledgePoint().isBlank()
                    ? "未标注知识点"
                    : question.getKnowledgePoint();
            counts.put(key, counts.getOrDefault(key, 0L) + 1);
        }
        return counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("knowledgePoint", entry.getKey());
                    row.put("wrongCount", entry.getValue());
                    return row;
                })
                .toList();
    }

    private double roundAverage(List<ExamAttempt> attempts) {
        double average = attempts.stream()
                .filter(a -> a.getScore() != null)
                .mapToInt(ExamAttempt::getScore)
                .average()
                .orElse(0);
        return Math.round(average * 10.0) / 10.0;
    }

    private String className(ClassInfo classInfo) {
        return java.util.stream.Stream.of(classInfo.getGrade(), classInfo.getMajor(), classInfo.getName())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
    }
}
