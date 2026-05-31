package com.campus.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exam.config.SparkProperties;
import com.campus.exam.dto.*;
import com.campus.exam.entity.Paper;
import com.campus.exam.entity.Question;
import com.campus.exam.entity.QuestionOption;
import com.campus.exam.mapper.CourseMapper;
import com.campus.exam.mapper.QuestionMapper;
import com.campus.exam.mapper.QuestionOptionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiAuthoringService {
    private final SparkProperties properties;
    private final CourseMapper courseMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate(requestFactory());

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return factory;
    }

    public List<QuestionDto> generateQuestions(AiQuestionGenerateRequest request) {
        int count = clamp(request.count(), 1, 10);
        String type = normalizeType(request.type());
        String courseName = Optional.ofNullable(courseMapper.selectById(request.courseId()))
                .map(c -> c.getName())
                .orElse("课程");
        String prompt = """
                请为高校在线考试系统生成客观题。只返回 JSON，不要 Markdown。
                JSON 格式：
                {"questions":[{"type":"SINGLE|MULTIPLE|JUDGE","stem":"题干","correctAnswer":"A","analysis":"解析","difficulty":1,"options":[{"optionKey":"A","optionText":"选项"}]}]}
                要求：
                1. 课程：%s
                2. 知识点：%s
                3. 题型：%s
                4. 数量：%d
                5. 难度：%d
                6. 单选和多选必须有 A-D 四个选项；判断题用 A.正确、B.错误；简答题和编程题不需要选项。
                7. 多选答案用逗号分隔，例如 A,C。
                """.formatted(courseName, emptyToDefault(request.topic(), "课程核心知识点"), type, count, clamp(request.difficulty(), 1, 5));
        try {
            return parseQuestions(callSpark(prompt), request.courseId(), type, count, request.difficulty());
        } catch (Exception ex) {
            return fallbackQuestions(request.courseId(), request.topic(), type, count, request.difficulty());
        }
    }

    public AiPaperDraft generatePaper(AiPaperGenerateRequest request) {
        List<Question> bank = questionMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getCourseId, request.courseId())
                .orderByAsc(Question::getDifficulty)
                .orderByDesc(Question::getId));
        if (bank.isEmpty()) {
            throw new IllegalArgumentException("该课程暂无题目，请先生成或录入题库");
        }
        int count = clamp(request.questionCount(), 1, bank.size());
        int totalScore = request.totalScore() == null || request.totalScore() <= 0 ? count * 10 : request.totalScore();
        String questionSummary = bank.stream()
                .map(q -> "ID=%d，题型=%s，难度=%d，题干=%s".formatted(q.getId(), q.getType(), q.getDifficulty(), q.getStem()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        String prompt = """
                请从下面题库中为在线考试系统生成一份试卷方案。只返回 JSON，不要 Markdown。
                JSON 格式：
                {"title":"试卷名称","durationMinutes":45,"questions":[{"questionId":1,"score":10,"sortNo":1}]}
                要求：
                1. 试卷名称：%s
                2. 考查重点：%s
                3. 题目数量：%d
                4. 总分：%d
                5. 只能选择题库中存在的 ID，题型尽量满足：单选%d、多选%d、判断%d、简答%d、编程%d。
                6. 难度尽量满足：简单%d、中等%d、困难%d；知识点覆盖：%s。
                题库：
                %s
                """.formatted(
                emptyToDefault(request.title(), "AI智能组卷"),
                emptyToDefault(request.topic(), "课程综合能力"),
                count,
                totalScore,
                defaultInt(request.singleCount()),
                defaultInt(request.multipleCount()),
                defaultInt(request.judgeCount()),
                defaultInt(request.shortCount()),
                defaultInt(request.programCount()),
                defaultInt(request.easyCount()),
                defaultInt(request.mediumCount()),
                defaultInt(request.hardCount()),
                request.knowledgePoints() == null ? "不限" : String.join("、", request.knowledgePoints()),
                questionSummary);
        try {
            return parsePaper(callSpark(prompt), request, bank, count, totalScore);
        } catch (Exception ex) {
            return fallbackPaper(request, bank, count, totalScore);
        }
    }

    private List<QuestionDto> parseQuestions(String raw, Long courseId, String forcedType, int count, Integer difficulty) throws Exception {
        JsonNode questions = objectMapper.readTree(extractJson(raw)).path("questions");
        if (!questions.isArray() || questions.isEmpty()) {
            throw new IllegalStateException("AI未返回题目数组");
        }
        List<QuestionDto> result = new ArrayList<>();
        for (JsonNode node : questions) {
            Question q = new Question();
            q.setCourseId(courseId);
            q.setType(normalizeType(node.path("type").asText(forcedType)));
            q.setStem(node.path("stem").asText());
            q.setCorrectAnswer(node.path("correctAnswer").asText());
            q.setAnalysis(node.path("analysis").asText());
            q.setDifficulty(clamp(node.path("difficulty").asInt(difficulty == null ? 1 : difficulty), 1, 5));
            List<QuestionOption> options = new ArrayList<>();
            JsonNode optionNodes = node.path("options");
            if (optionNodes.isArray()) {
                for (JsonNode optionNode : optionNodes) {
                    QuestionOption option = new QuestionOption();
                    option.setOptionKey(optionNode.path("optionKey").asText());
                    option.setOptionText(optionNode.path("optionText").asText());
                    options.add(option);
                }
            }
            ensureOptions(q.getType(), options);
            result.add(new QuestionDto(q, options));
            if (result.size() >= count) {
                break;
            }
        }
        return result;
    }

    private AiPaperDraft parsePaper(String raw, AiPaperGenerateRequest request, List<Question> bank, int count, int totalScore) throws Exception {
        Set<Long> validIds = new HashSet<>();
        bank.forEach(q -> validIds.add(q.getId()));
        JsonNode root = objectMapper.readTree(extractJson(raw));
        List<PaperQuestionRequest> questions = new ArrayList<>();
        int sort = 1;
        for (JsonNode item : root.path("questions")) {
            long id = item.path("questionId").asLong();
            if (validIds.contains(id) && questions.stream().noneMatch(q -> q.questionId().equals(id))) {
                questions.add(new PaperQuestionRequest(id, Math.max(1, item.path("score").asInt(10)), sort++));
            }
            if (questions.size() >= count) {
                break;
            }
        }
        if (questions.isEmpty()) {
            throw new IllegalStateException("AI未返回可用题目");
        }
        normalizeScores(questions, totalScore);
        Paper paper = new Paper();
        paper.setCourseId(request.courseId());
        paper.setTitle(emptyToDefault(root.path("title").asText(), emptyToDefault(request.title(), "AI智能组卷")));
        paper.setDurationMinutes(request.durationMinutes() == null ? root.path("durationMinutes").asInt(45) : request.durationMinutes());
        paper.setPublished(0);
        paper.setTotalScore(totalScore);
        return new AiPaperDraft(paper, questions);
    }

    private List<QuestionDto> fallbackQuestions(Long courseId, String topic, String type, int count, Integer difficulty) {
        List<QuestionDto> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Question q = new Question();
            q.setCourseId(courseId);
            q.setType(type);
            q.setStem("%s相关知识点练习题 %d".formatted(emptyToDefault(topic, "课程"), i));
            q.setCorrectAnswer("JUDGE".equals(type) ? "A" : "A");
            q.setAnalysis("本题用于考查%s的核心概念，请结合教材定义和课堂案例分析。".formatted(emptyToDefault(topic, "课程知识点")));
            q.setDifficulty(clamp(difficulty, 1, 5));
            List<QuestionOption> options = new ArrayList<>();
            ensureOptions(type, options);
            list.add(new QuestionDto(q, options));
        }
        return list;
    }

    private AiPaperDraft fallbackPaper(AiPaperGenerateRequest request, List<Question> bank, int count, int totalScore) {
        List<Question> selected = bank.stream()
                .filter(q -> matchesKnowledge(request, q))
                .sorted(Comparator.comparing((Question q) -> typePenalty(request, q))
                        .thenComparing(q -> difficultyPenalty(request, q))
                        .thenComparing(Question::getDifficulty)
                        .thenComparing(Question::getType))
                .limit(count)
                .toList();
        if (selected.size() < count) {
            selected = bank.stream()
                    .sorted(Comparator.comparing(Question::getDifficulty).thenComparing(Question::getType))
                    .limit(count)
                    .toList();
        }
        int baseScore = Math.max(1, totalScore / selected.size());
        List<PaperQuestionRequest> links = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            links.add(new PaperQuestionRequest(selected.get(i).getId(), baseScore, i + 1));
        }
        normalizeScores(links, totalScore);
        Paper paper = new Paper();
        paper.setCourseId(request.courseId());
        paper.setTitle(emptyToDefault(request.title(), "AI智能组卷"));
        paper.setDurationMinutes(request.durationMinutes() == null ? 45 : request.durationMinutes());
        paper.setPublished(0);
        paper.setTotalScore(totalScore);
        return new AiPaperDraft(paper, links);
    }

    private String callSpark(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.apiPassword());
        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是高校教师的命题与组卷助手，输出必须严格遵守 JSON 格式。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.35
        );
        ResponseEntity<String> response = restTemplate.exchange(properties.apiUrl(), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        JsonNode content = objectMapper.readTree(response.getBody()).path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("AI接口未返回内容");
        }
        return content.asText();
    }

    private String extractJson(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json", "").replaceFirst("^```", "");
            int end = text.lastIndexOf("```");
            if (end >= 0) {
                text = text.substring(0, end);
            }
        }
        int objectStart = text.indexOf('{');
        int objectEnd = text.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return text.substring(objectStart, objectEnd + 1);
        }
        return text;
    }

    private void ensureOptions(String type, List<QuestionOption> options) {
        if (!options.isEmpty()) {
            return;
        }
        if ("SHORT".equals(type) || "PROGRAM".equals(type)) {
            return;
        }
        if ("JUDGE".equals(type)) {
            options.add(option("A", "正确"));
            options.add(option("B", "错误"));
        } else {
            options.add(option("A", "正确选项"));
            options.add(option("B", "干扰选项一"));
            options.add(option("C", "干扰选项二"));
            options.add(option("D", "干扰选项三"));
        }
    }

    private QuestionOption option(String key, String text) {
        QuestionOption option = new QuestionOption();
        option.setOptionKey(key);
        option.setOptionText(text);
        return option;
    }

    private void normalizeScores(List<PaperQuestionRequest> questions, int totalScore) {
        int current = questions.stream().mapToInt(q -> q.score() == null ? 0 : q.score()).sum();
        if (current == totalScore || questions.isEmpty()) {
            return;
        }
        PaperQuestionRequest last = questions.get(questions.size() - 1);
        questions.set(questions.size() - 1, new PaperQuestionRequest(last.questionId(), Math.max(1, last.score() + totalScore - current), last.sortNo()));
    }

    private String normalizeType(String type) {
        if (List.of("SINGLE", "MULTIPLE", "JUDGE", "SHORT", "PROGRAM").contains(type)) {
            return type;
        }
        return "SINGLE";
    }

    private int typePenalty(AiPaperGenerateRequest request, Question question) {
        int target = switch (question.getType()) {
            case "SINGLE" -> defaultInt(request.singleCount());
            case "MULTIPLE" -> defaultInt(request.multipleCount());
            case "JUDGE" -> defaultInt(request.judgeCount());
            case "SHORT" -> defaultInt(request.shortCount());
            case "PROGRAM" -> defaultInt(request.programCount());
            default -> 0;
        };
        return target > 0 ? 0 : 1;
    }

    private int difficultyPenalty(AiPaperGenerateRequest request, Question question) {
        int difficulty = question.getDifficulty() == null ? 1 : question.getDifficulty();
        boolean wanted = difficulty <= 2 && defaultInt(request.easyCount()) > 0
                || difficulty == 3 && defaultInt(request.mediumCount()) > 0
                || difficulty >= 4 && defaultInt(request.hardCount()) > 0;
        return wanted ? 0 : 1;
    }

    private boolean matchesKnowledge(AiPaperGenerateRequest request, Question question) {
        if (request.knowledgePoints() == null || request.knowledgePoints().isEmpty()) {
            return true;
        }
        String point = question.getKnowledgePoint() == null ? "" : question.getKnowledgePoint();
        return request.knowledgePoints().stream().anyMatch(point::contains);
    }

    private int clamp(Integer value, int min, int max) {
        int v = value == null ? min : value;
        return Math.max(min, Math.min(max, v));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String emptyToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
