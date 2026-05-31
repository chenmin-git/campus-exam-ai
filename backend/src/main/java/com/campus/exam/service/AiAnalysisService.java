package com.campus.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exam.config.SparkProperties;
import com.campus.exam.entity.AiAnalysisCache;
import com.campus.exam.entity.Question;
import com.campus.exam.entity.QuestionOption;
import com.campus.exam.mapper.AiAnalysisCacheMapper;
import com.campus.exam.mapper.QuestionMapper;
import com.campus.exam.mapper.QuestionOptionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiAnalysisService {
    private final SparkProperties properties;
    private final AiAnalysisCacheMapper cacheMapper;
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

    public String analyze(Long questionId, String studentAnswer) {
        String normalizedAnswer = studentAnswer == null ? "" : studentAnswer;
        AiAnalysisCache cached = cacheMapper.selectOne(new LambdaQueryWrapper<AiAnalysisCache>()
                .eq(AiAnalysisCache::getQuestionId, questionId)
                .eq(AiAnalysisCache::getStudentAnswer, normalizedAnswer)
                .last("limit 1"));
        if (cached != null) {
            return cached.getContent();
        }
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        List<QuestionOption> options = optionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, questionId)
                .orderByAsc(QuestionOption::getOptionKey));
        String prompt = """
                请作为校园在线考试系统的错题解析助手，基于下面题目信息输出中文解析。
                要求包含：1. 涉及知识点；2. 学生错误原因；3. 正确解题思路；4. 正确答案说明。
                题型：%s
                题干：%s
                选项：%s
                标准答案：%s
                学生答案：%s
                """.formatted(question.getType(), question.getStem(), formatOptions(options), question.getCorrectAnswer(), normalizedAnswer);
        String content;
        try {
            content = callSpark(prompt);
        } catch (Exception ex) {
            content = fallback(question, normalizedAnswer);
        }
        AiAnalysisCache cache = new AiAnalysisCache();
        cache.setQuestionId(questionId);
        cache.setStudentAnswer(normalizedAnswer);
        cache.setContent(content);
        cache.setCreatedAt(LocalDateTime.now());
        cacheMapper.insert(cache);
        return content;
    }

    public String studyPlan(List<Map<String, Object>> weakRows) {
        String summary = weakRows.stream()
                .map(row -> row.get("knowledgePoint") + " 错题数：" + row.get("wrongCount"))
                .collect(Collectors.joining("；"));
        String prompt = """
                请作为校园在线考试系统的智能学习顾问，根据学生错题知识点生成中文复习建议。
                要求：1. 先给出优先复习顺序；2. 每个知识点给出复习方法；3. 给出一周练习安排；4. 语言简洁。
                错题统计：%s
                """.formatted(summary.isBlank() ? "暂无错题" : summary);
        try {
            return callSpark(prompt);
        } catch (Exception ex) {
            if (weakRows.isEmpty()) {
                return "当前没有明显薄弱知识点，建议保持练习节奏，考前复盘历史试卷。";
            }
            return "建议优先复习：" + summary + "。每天选择 2-3 道同知识点题目进行限时训练，并复述错因。";
        }
    }

    public String quickFallback(Question question, String studentAnswer) {
        return fallback(question, studentAnswer);
    }

    private String callSpark(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.apiPassword());
        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是严谨、易懂的大学课程助教。"),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.4
        );
        ResponseEntity<String> response = restTemplate.exchange(
                properties.apiUrl(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IllegalStateException("AI接口未返回解析内容");
        }
        return content.asText();
    }

    private String fallback(Question question, String studentAnswer) {
        return "【AI解析暂不可用，已生成本地解析】\n"
                + "知识点：" + question.getStem() + "\n"
                + "你的答案：" + (studentAnswer.isBlank() ? "未作答" : studentAnswer) + "\n"
                + "正确答案：" + question.getCorrectAnswer() + "\n"
                + "解析：" + (question.getAnalysis() == null || question.getAnalysis().isBlank() ? "请回到教材对应章节复习概念，并比较各选项差异。" : question.getAnalysis());
    }

    private String formatOptions(List<QuestionOption> options) {
        return options.stream()
                .map(o -> o.getOptionKey() + "." + o.getOptionText())
                .collect(Collectors.joining("；"));
    }
}
