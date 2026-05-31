package com.campus.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exam.dto.AppealRequest;
import com.campus.exam.dto.AppealReviewRequest;
import com.campus.exam.dto.ManualGradeRequest;
import com.campus.exam.dto.MonitorEventRequest;
import com.campus.exam.dto.SubmitExamRequest;
import com.campus.exam.entity.*;
import com.campus.exam.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {
    private final PaperMapper paperMapper;
    private final PaperQuestionMapper paperQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final ExamAttemptMapper attemptMapper;
    private final StudentAnswerMapper answerMapper;
    private final UserMapper userMapper;
    private final TeacherCourseMapper teacherCourseMapper;
    private final ClassInfoMapper classInfoMapper;
    private final CourseMapper courseMapper;
    private final ExamMonitorEventMapper monitorEventMapper;
    private final GradeAppealMapper gradeAppealMapper;
    private final NotificationService notificationService;
    private final OperationLogService operationLogService;
    private final AiAnalysisService aiAnalysisService;

    public List<Paper> availablePapers(User current) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<Paper>()
                .eq(Paper::getPublished, 1)
                .and(w -> w.isNull(Paper::getStartTime).or().le(Paper::getStartTime, now))
                .and(w -> w.isNull(Paper::getEndTime).or().ge(Paper::getEndTime, now))
                .orderByDesc(Paper::getId);
        if ("TEACHER".equals(current.getRole())) {
            wrapper.eq(Paper::getCreatorId, current.getId());
        } else if ("STUDENT".equals(current.getRole())) {
            Long classId = userMapper.selectById(current.getId()).getClassId();
            if (classId != null) {
                Set<Long> courseIds = teacherCourseMapper.selectList(new LambdaQueryWrapper<TeacherCourse>()
                                .eq(TeacherCourse::getClassId, classId))
                        .stream().map(TeacherCourse::getCourseId).collect(Collectors.toSet());
                if (courseIds.isEmpty()) {
                    return List.of();
                }
                wrapper.in(Paper::getCourseId, courseIds);
            }
        }
        return paperMapper.selectList(wrapper);
    }

    @Transactional
    public Map<String, Object> start(Long paperId, Long studentId) {
        Paper paper = paperMapper.selectById(paperId);
        if (paper == null || paper.getPublished() == null || paper.getPublished() != 1) {
            throw new IllegalArgumentException("试卷不存在或未发布");
        }
        ensurePaperOpen(paper);
        ExamAttempt latest = attemptMapper.selectOne(new LambdaQueryWrapper<ExamAttempt>()
                .eq(ExamAttempt::getPaperId, paperId)
                .eq(ExamAttempt::getStudentId, studentId)
                .orderByDesc(ExamAttempt::getId)
                .last("limit 1"));
        if (latest != null && "IN_PROGRESS".equals(latest.getStatus())) {
            if (latest.getDueAt() != null && latest.getDueAt().isBefore(LocalDateTime.now())) {
                latest.setStatus("EXPIRED");
                attemptMapper.updateById(latest);
                throw new IllegalArgumentException("该考试已超时，不能继续作答");
            }
            return paperPayload(paper, latest, false);
        }
        if (latest != null && !"IN_PROGRESS".equals(latest.getStatus()) && !Integer.valueOf(1).equals(paper.getAllowRetake())) {
            throw new IllegalArgumentException("该试卷已参加，不能重复考试");
        }
        ExamAttempt attempt = new ExamAttempt();
        attempt.setPaperId(paperId);
        attempt.setStudentId(studentId);
        attempt.setStatus("IN_PROGRESS");
        attempt.setScore(0);
        attempt.setObjectiveScore(0);
        attempt.setSubjectiveScore(0);
        attempt.setReviewStatus("NONE");
        attempt.setStartedAt(LocalDateTime.now());
        LocalDateTime dueAt = LocalDateTime.now().plusMinutes(paper.getDurationMinutes() == null ? 60 : paper.getDurationMinutes());
        if (paper.getEndTime() != null && paper.getEndTime().isBefore(dueAt)) {
            dueAt = paper.getEndTime();
        }
        attempt.setDueAt(dueAt);
        attemptMapper.insert(attempt);
        operationLogService.log("开始考试", "attempt:" + attempt.getId(), "paper:" + paperId);
        return paperPayload(paper, attempt, false);
    }

    public Map<String, Object> paperPayload(Paper paper, ExamAttempt attempt, boolean includeAnswer) {
        List<PaperQuestion> links = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paper.getId())
                .orderByAsc(PaperQuestion::getSortNo));
        List<Map<String, Object>> questions = new ArrayList<>();
        for (PaperQuestion link : links) {
            Question question = questionMapper.selectById(link.getQuestionId());
            if (question == null) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", question.getId());
            item.put("type", question.getType());
            item.put("stem", question.getStem());
            item.put("score", link.getScore());
            item.put("knowledgePoint", question.getKnowledgePoint());
            item.put("options", QuestionService.isObjective(question.getType())
                    ? optionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                    .eq(QuestionOption::getQuestionId, question.getId())
                    .orderByAsc(QuestionOption::getOptionKey))
                    : List.of());
            if (includeAnswer) {
                item.put("correctAnswer", question.getCorrectAnswer());
                item.put("analysis", question.getAnalysis());
            }
            questions.add(item);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attempt", attempt);
        payload.put("paper", paper);
        payload.put("questions", questions);
        return payload;
    }

    @Transactional
    public ExamAttempt submit(Long attemptId, SubmitExamRequest request, Long studentId) {
        ExamAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null || !Objects.equals(attempt.getStudentId(), studentId)) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        if (attempt.getDueAt() != null && attempt.getDueAt().plusSeconds(30).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("考试已超时，不能提交");
        }
        if ("SUBMITTED".equals(attempt.getStatus())) {
            return attempt;
        }
        Map<Long, String> answerMap = request.answers() == null ? Map.of() : request.answers().stream()
                .collect(Collectors.toMap(SubmitExamRequest.AnswerItem::questionId, a -> normalize(a.answer()), (a, b) -> b));
        List<PaperQuestion> links = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, attempt.getPaperId()));
        answerMapper.delete(new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getAttemptId, attemptId));
        int total = 0;
        int objectiveScore = 0;
        int subjectiveScore = 0;
        boolean pendingReview = false;
        for (PaperQuestion link : links) {
            Question question = questionMapper.selectById(link.getQuestionId());
            String studentAnswer = answerMap.getOrDefault(question.getId(), "");
            boolean objective = QuestionService.isObjective(question.getType());
            boolean correct = objective && normalize(question.getCorrectAnswer()).equals(studentAnswer);
            int score = correct ? Optional.ofNullable(link.getScore()).orElse(0) : 0;
            if (objective) {
                objectiveScore += score;
                total += score;
            } else {
                pendingReview = true;
                subjectiveScore += 0;
            }
            StudentAnswer entity = new StudentAnswer();
            entity.setAttemptId(attemptId);
            entity.setQuestionId(question.getId());
            entity.setAnswer(studentAnswer);
            entity.setCorrect(correct ? 1 : 0);
            entity.setScore(score);
            entity.setManualScore(objective ? score : 0);
            entity.setReviewStatus(objective ? "AUTO" : "PENDING");
            answerMapper.insert(entity);
        }
        attempt.setScore(total);
        attempt.setObjectiveScore(objectiveScore);
        attempt.setSubjectiveScore(subjectiveScore);
        attempt.setStatus(pendingReview ? "PENDING_REVIEW" : "SUBMITTED");
        attempt.setReviewStatus(pendingReview ? "PENDING" : "DONE");
        attempt.setSubmittedAt(LocalDateTime.now());
        attemptMapper.updateById(attempt);
        notificationService.create(attempt.getStudentId(), null, "考试提交成功", "你在《" + paperMapper.selectById(attempt.getPaperId()).getTitle() + "》中的提交已完成。");
        operationLogService.log("提交考试", "attempt:" + attemptId, "score=" + total);
        return attempt;
    }

    public List<ExamAttempt> attempts(Long studentId) {
        return attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>()
                .eq(ExamAttempt::getStudentId, studentId)
                .orderByDesc(ExamAttempt::getId));
    }

    public List<Map<String, Object>> wrongQuestions(Long studentId) {
        List<ExamAttempt> attempts = attempts(studentId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            List<StudentAnswer> wrong = answerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>()
                    .eq(StudentAnswer::getAttemptId, attempt.getId())
                    .eq(StudentAnswer::getCorrect, 0)
                    .ne(StudentAnswer::getReviewStatus, "PENDING"));
            for (StudentAnswer answer : wrong) {
                Question question = questionMapper.selectById(answer.getQuestionId());
                if (question == null) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("attemptId", attempt.getId());
                row.put("question", question);
                row.put("studentAnswer", answer.getAnswer());
                row.put("options", optionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                        .eq(QuestionOption::getQuestionId, question.getId())
                        .orderByAsc(QuestionOption::getOptionKey)));
                result.add(row);
            }
        }
        return result;
    }

    public List<ExamAttempt> allAttempts() {
        return attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>().orderByDesc(ExamAttempt::getId));
    }

    public List<Map<String, Object>> scoreRows() {
        return allAttempts().stream().map(attempt -> {
            Map<String, Object> row = new LinkedHashMap<>();
            Paper paper = paperMapper.selectById(attempt.getPaperId());
            User student = userMapper.selectById(attempt.getStudentId());
            row.put("id", attempt.getId());
            row.put("paperId", attempt.getPaperId());
            row.put("paperTitle", paper == null ? "已删除试卷" : paper.getTitle());
            row.put("studentId", attempt.getStudentId());
            row.put("studentName", student == null ? "未知学生" : student.getRealName());
            row.put("score", attempt.getScore());
            row.put("status", attempt.getStatus());
            row.put("startedAt", attempt.getStartedAt());
            row.put("submittedAt", attempt.getSubmittedAt());
            return row;
        }).toList();
    }

    public List<Map<String, Object>> scoreRows(User current) {
        if (current == null || "ADMIN".equals(current.getRole())) {
            return scoreRows();
        }
        Set<Long> paperIds = paperMapper.selectList(new LambdaQueryWrapper<Paper>()
                        .eq(Paper::getCreatorId, current.getId()))
                .stream().map(Paper::getId).collect(Collectors.toSet());
        if (paperIds.isEmpty()) {
            return List.of();
        }
        return allAttempts().stream()
                .filter(a -> paperIds.contains(a.getPaperId()))
                .map(attempt -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    Paper paper = paperMapper.selectById(attempt.getPaperId());
                    User student = userMapper.selectById(attempt.getStudentId());
                    row.put("id", attempt.getId());
                    row.put("paperId", attempt.getPaperId());
                    row.put("paperTitle", paper == null ? "已删除试卷" : paper.getTitle());
                    row.put("studentId", attempt.getStudentId());
                    row.put("studentName", student == null ? "未知学生" : student.getRealName());
                    row.put("score", attempt.getScore());
                    row.put("objectiveScore", attempt.getObjectiveScore());
                    row.put("subjectiveScore", attempt.getSubjectiveScore());
                    row.put("status", attempt.getStatus());
                    row.put("reviewStatus", attempt.getReviewStatus());
                    row.put("startedAt", attempt.getStartedAt());
                    row.put("submittedAt", attempt.getSubmittedAt());
                    return row;
                }).toList();
    }

    public List<Map<String, Object>> pendingManualAnswers(User current) {
        Set<Long> paperIds = paperMapper.selectList(new LambdaQueryWrapper<Paper>()
                        .eq(Paper::getCreatorId, current.getId()))
                .stream().map(Paper::getId).collect(Collectors.toSet());
        if (paperIds.isEmpty()) {
            return List.of();
        }
        List<ExamAttempt> attempts = attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>()
                .in(ExamAttempt::getPaperId, paperIds)
                .in(ExamAttempt::getStatus, List.of("PENDING_REVIEW", "SUBMITTED")));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamAttempt attempt : attempts) {
            List<StudentAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>()
                    .eq(StudentAnswer::getAttemptId, attempt.getId())
                    .eq(StudentAnswer::getReviewStatus, "PENDING"));
            for (StudentAnswer answer : answers) {
                Question question = questionMapper.selectById(answer.getQuestionId());
                if (question == null) {
                    continue;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("attemptId", attempt.getId());
                row.put("answerId", answer.getId());
                row.put("paperId", attempt.getPaperId());
                row.put("studentId", attempt.getStudentId());
                row.put("question", question);
                row.put("maxScore", paperQuestionScore(attempt.getPaperId(), question.getId()));
                row.put("studentAnswer", answer.getAnswer());
                row.put("score", answer.getScore());
                row.put("manualScore", answer.getManualScore());
                row.put("comment", answer.getTeacherComment());
                result.add(row);
            }
        }
        return result;
    }

    @Transactional
    public StudentAnswer manualGrade(Long answerId, ManualGradeRequest request, Long teacherId) {
        StudentAnswer answer = answerMapper.selectById(answerId);
        if (answer == null) {
            throw new IllegalArgumentException("答题记录不存在");
        }
        if (request.score() == null || request.score() < 0) {
            throw new IllegalArgumentException("分数不能小于 0");
        }
        ExamAttempt attempt = attemptMapper.selectById(answer.getAttemptId());
        if (attempt == null) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        PaperQuestion link = paperQuestionMapper.selectOne(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, attempt.getPaperId())
                .eq(PaperQuestion::getQuestionId, answer.getQuestionId())
                .last("limit 1"));
        int maxScore = link == null ? request.score() : Optional.ofNullable(link.getScore()).orElse(request.score());
        int finalScore = Math.min(request.score(), maxScore);
        answer.setManualScore(finalScore);
        answer.setScore(finalScore);
        answer.setTeacherComment(request.comment());
        answer.setReviewStatus("REVIEWED");
        answerMapper.updateById(answer);
        recalcAttemptScore(answer.getAttemptId());
        if (attempt != null) {
            notificationService.create(attempt.getStudentId(), null, "试卷已复核", "你在《" + paperMapper.selectById(attempt.getPaperId()).getTitle() + "》中的主观题已被教师复核。");
        }
        operationLogService.log("人工阅卷", "answer:" + answerId, "score=" + request.score());
        return answer;
    }

    public List<Map<String, Object>> analysisReport(User current) {
        Set<Long> paperIds = paperMapper.selectList(new LambdaQueryWrapper<Paper>()
                        .eq(Paper::getCreatorId, current.getId()))
                .stream().map(Paper::getId).collect(Collectors.toSet());
        if (paperIds.isEmpty()) {
            return List.of();
        }
        List<ExamAttempt> attempts = attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>().in(ExamAttempt::getPaperId, paperIds));
        Map<Long, List<ExamAttempt>> byPaper = attempts.stream().collect(Collectors.groupingBy(ExamAttempt::getPaperId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long paperId : paperIds) {
            List<ExamAttempt> list = byPaper.getOrDefault(paperId, List.of());
            Map<String, Object> row = new LinkedHashMap<>();
            Paper paper = paperMapper.selectById(paperId);
            row.put("paperId", paperId);
            row.put("paperTitle", paper == null ? "已删除试卷" : paper.getTitle());
            row.put("count", list.size());
            row.put("averageScore", round(list.stream().filter(a -> a.getScore() != null).mapToInt(ExamAttempt::getScore).average().orElse(0)));
            row.put("maxScore", list.stream().mapToInt(a -> a.getScore() == null ? 0 : a.getScore()).max().orElse(0));
            row.put("minScore", list.stream().mapToInt(a -> a.getScore() == null ? 0 : a.getScore()).min().orElse(0));
            row.put("passRate", list.isEmpty() ? 0 : round((double) list.stream().filter(a -> (a.getScore() != null ? a.getScore() : 0) >= (paper == null || paper.getTotalScore() == null ? 60 : paper.getTotalScore() * 0.6)).count() * 100 / list.size()));
            row.put("questionAccuracy", questionAccuracy(paperId));
            row.put("weakKnowledgePoints", weakKnowledgePoints(paperId));
            result.add(row);
        }
        return result;
    }

    public List<Map<String, Object>> monitorEvents(User current) {
        Set<Long> paperIds = paperMapper.selectList(new LambdaQueryWrapper<Paper>()
                        .eq(Paper::getCreatorId, current.getId()))
                .stream().map(Paper::getId).collect(Collectors.toSet());
        if (paperIds.isEmpty()) {
            return List.of();
        }
        return monitorEventMapper.selectList(new LambdaQueryWrapper<ExamMonitorEvent>()
                        .in(ExamMonitorEvent::getPaperId, paperIds)
                        .orderByDesc(ExamMonitorEvent::getId))
                .stream().map(event -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", event.getId());
                    row.put("attemptId", event.getAttemptId());
                    row.put("paperId", event.getPaperId());
                    row.put("studentId", event.getStudentId());
                    row.put("eventType", event.getEventType());
                    row.put("detail", event.getDetail());
                    row.put("ip", event.getIp());
                    row.put("userAgent", event.getUserAgent());
                    row.put("createdAt", event.getCreatedAt());
                    return row;
                }).toList();
    }

    @Transactional
    public ExamMonitorEvent recordMonitor(MonitorEventRequest request, HttpServletRequest httpRequest, Long studentId) {
        ExamAttempt attempt = attemptMapper.selectById(request.attemptId());
        if (attempt == null || !Objects.equals(attempt.getStudentId(), studentId)) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        Paper paper = paperMapper.selectById(attempt.getPaperId());
        ExamMonitorEvent event = new ExamMonitorEvent();
        event.setAttemptId(attempt.getId());
        event.setPaperId(attempt.getPaperId());
        event.setStudentId(studentId);
        event.setEventType(request.eventType());
        event.setDetail(request.detail());
        event.setIp(httpRequest.getRemoteAddr());
        event.setUserAgent(httpRequest.getHeader("User-Agent"));
        event.setCreatedAt(LocalDateTime.now());
        monitorEventMapper.insert(event);
        operationLogService.log("考试监控", "attempt:" + attempt.getId(), request.eventType() + ":" + request.detail());
        if (paper != null && request.eventType() != null && List.of("BLUR", "VISIBILITY_CHANGE", "FULLSCREEN_EXIT").contains(request.eventType())) {
            notificationService.create(attempt.getStudentId(), null, "考试异常提醒", "系统检测到你的考试存在异常行为：" + request.eventType());
        }
        return event;
    }

    public List<GradeAppeal> appeals(User current) {
        if ("TEACHER".equals(current.getRole()) || "ADMIN".equals(current.getRole())) {
            return gradeAppealMapper.selectList(new LambdaQueryWrapper<GradeAppeal>().orderByDesc(GradeAppeal::getId));
        }
        return gradeAppealMapper.selectList(new LambdaQueryWrapper<GradeAppeal>()
                .eq(GradeAppeal::getStudentId, current.getId())
                .orderByDesc(GradeAppeal::getId));
    }

    @Transactional
    public GradeAppeal createAppeal(AppealRequest request, Long studentId) {
        ExamAttempt attempt = attemptMapper.selectById(request.attemptId());
        if (attempt == null || !Objects.equals(attempt.getStudentId(), studentId)) {
            throw new IllegalArgumentException("考试记录不存在");
        }
        GradeAppeal appeal = new GradeAppeal();
        appeal.setAttemptId(request.attemptId());
        appeal.setStudentId(studentId);
        appeal.setReason(request.reason());
        appeal.setStatus("PENDING");
        appeal.setCreatedAt(LocalDateTime.now());
        gradeAppealMapper.insert(appeal);
        operationLogService.log("创建申诉", "appeal:" + appeal.getId(), request.reason());
        return appeal;
    }

    @Transactional
    public GradeAppeal reviewAppeal(Long id, AppealReviewRequest request, Long teacherId) {
        GradeAppeal appeal = gradeAppealMapper.selectById(id);
        if (appeal == null) {
            throw new IllegalArgumentException("申诉不存在");
        }
        appeal.setStatus(request.status() == null ? "PENDING" : request.status());
        appeal.setReply(request.reply());
        appeal.setReviewerId(teacherId);
        appeal.setReviewedAt(LocalDateTime.now());
        gradeAppealMapper.updateById(appeal);
        notificationService.create(appeal.getStudentId(), null, "成绩申诉处理结果", "你的申诉已处理，结果：" + appeal.getStatus());
        operationLogService.log("处理申诉", "appeal:" + id, appeal.getStatus());
        return appeal;
    }

    public List<Map<String, Object>> studyAdvice(Long studentId) {
        List<Map<String, Object>> wrongs = wrongQuestions(studentId);
        Map<String, Long> byKnowledge = new LinkedHashMap<>();
        for (Map<String, Object> item : wrongs) {
            Question question = (Question) item.get("question");
            String key = question.getKnowledgePoint() == null || question.getKnowledgePoint().isBlank() ? "未标注知识点" : question.getKnowledgePoint();
            byKnowledge.put(key, byKnowledge.getOrDefault(key, 0L) + 1);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : byKnowledge.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("knowledgePoint", entry.getKey());
            row.put("wrongCount", entry.getValue());
            row.put("advice", "建议优先复习 " + entry.getKey() + "，结合错题重新做一遍相关练习。");
            result.add(row);
        }
        if (result.isEmpty()) {
            result.add(Map.of("knowledgePoint", "暂无错题", "wrongCount", 0, "advice", "继续保持，当前没有需要重点复习的错题。"));
        }
        long totalWrong = result.stream().mapToLong(row -> Long.parseLong(String.valueOf(row.get("wrongCount")))).sum();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("knowledgePoint", "AI复习计划");
        summary.put("wrongCount", totalWrong);
        summary.put("advice", aiAnalysisService.studyPlan(result));
        result.add(0, summary);
        return result;
    }

    private void ensurePaperOpen(Paper paper) {
        LocalDateTime now = LocalDateTime.now();
        if (paper.getStartTime() != null && now.isBefore(paper.getStartTime())) {
            throw new IllegalArgumentException("考试尚未开始");
        }
        if (paper.getEndTime() != null && now.isAfter(paper.getEndTime())) {
            throw new IllegalArgumentException("考试已结束");
        }
    }

    private void recalcAttemptScore(Long attemptId) {
        List<StudentAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>()
                .eq(StudentAnswer::getAttemptId, attemptId));
        int total = answers.stream().mapToInt(a -> a.getScore() == null ? 0 : a.getScore()).sum();
        int objective = answers.stream().filter(a -> a.getReviewStatus() == null || "AUTO".equals(a.getReviewStatus())).mapToInt(a -> a.getScore() == null ? 0 : a.getScore()).sum();
        int subjective = answers.stream().filter(a -> "REVIEWED".equals(a.getReviewStatus())).mapToInt(a -> a.getScore() == null ? 0 : a.getScore()).sum();
        ExamAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) {
            return;
        }
        attempt.setScore(total);
        attempt.setObjectiveScore(objective);
        attempt.setSubjectiveScore(subjective);
        boolean pending = answers.stream().anyMatch(a -> "PENDING".equals(a.getReviewStatus()));
        attempt.setStatus(pending ? "PENDING_REVIEW" : "SUBMITTED");
        attempt.setReviewStatus(pending ? "PENDING" : "DONE");
        attemptMapper.updateById(attempt);
    }

    private int paperQuestionScore(Long paperId, Long questionId) {
        PaperQuestion link = paperQuestionMapper.selectOne(new LambdaQueryWrapper<PaperQuestion>()
                .eq(PaperQuestion::getPaperId, paperId)
                .eq(PaperQuestion::getQuestionId, questionId)
                .last("limit 1"));
        return link == null || link.getScore() == null ? 0 : link.getScore();
    }

    private List<Map<String, Object>> questionAccuracy(Long paperId) {
        List<PaperQuestion> links = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paperId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PaperQuestion link : links) {
            List<StudentAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<StudentAnswer>().eq(StudentAnswer::getQuestionId, link.getQuestionId()));
            long correct = answers.stream().filter(a -> Integer.valueOf(1).equals(a.getCorrect())).count();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("questionId", link.getQuestionId());
            row.put("accuracy", answers.isEmpty() ? 0 : round(correct * 100.0 / answers.size()));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> weakKnowledgePoints(Long paperId) {
        List<PaperQuestion> links = paperQuestionMapper.selectList(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, paperId));
        Map<String, Long> map = new LinkedHashMap<>();
        for (PaperQuestion link : links) {
            Question q = questionMapper.selectById(link.getQuestionId());
            if (q == null || q.getKnowledgePoint() == null || q.getKnowledgePoint().isBlank()) {
                continue;
            }
            long wrong = answerMapper.selectCount(new LambdaQueryWrapper<StudentAnswer>()
                    .eq(StudentAnswer::getQuestionId, q.getId())
                    .eq(StudentAnswer::getCorrect, 0));
            if (wrong > 0) {
                map.put(q.getKnowledgePoint(), map.getOrDefault(q.getKnowledgePoint(), 0L) + wrong);
            }
        }
        return map.entrySet().stream().map(entry -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("knowledgePoint", entry.getKey());
            row.put("wrongCount", entry.getValue());
            return row;
        }).toList();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return Arrays.stream(raw.split("[,，\\s]+"))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .sorted()
                .collect(Collectors.joining(","));
    }
}
