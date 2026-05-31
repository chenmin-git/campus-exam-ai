package com.campus.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.exam.dto.QuestionDto;
import com.campus.exam.entity.Question;
import com.campus.exam.entity.QuestionOption;
import com.campus.exam.entity.User;
import com.campus.exam.mapper.QuestionMapper;
import com.campus.exam.mapper.QuestionOptionMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;
    private static final Set<String> OBJECTIVE_TYPES = Set.of("SINGLE", "MULTIPLE", "JUDGE");

    public List<Question> list(Long courseId, User current) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<Question>()
                .eq(Question::getDeleted, 0)
                .orderByDesc(Question::getId);
        if (courseId != null) {
            wrapper.eq(Question::getCourseId, courseId);
        }
        if ("TEACHER".equals(current.getRole())) {
            wrapper.eq(Question::getCreatorId, current.getId());
        }
        return questionMapper.selectList(wrapper);
    }

    public QuestionDto detail(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null || Integer.valueOf(1).equals(question.getDeleted())) {
            throw new IllegalArgumentException("题目不存在");
        }
        return new QuestionDto(question, options(id));
    }

    public List<QuestionOption> options(Long questionId) {
        return optionMapper.selectList(new LambdaQueryWrapper<QuestionOption>()
                .eq(QuestionOption::getQuestionId, questionId)
                .orderByAsc(QuestionOption::getOptionKey));
    }

    @Transactional
    public QuestionDto save(QuestionDto dto, Long creatorId) {
        Question q = dto.question();
        if (q.getCourseId() == null || q.getStem() == null || q.getStem().isBlank()) {
            throw new IllegalArgumentException("课程和题干不能为空");
        }
        q.setType(normalizeType(q.getType()));
        if (q.getCorrectAnswer() == null) {
            q.setCorrectAnswer("");
        }
        if (q.getDifficulty() == null) {
            q.setDifficulty(1);
        }
        if (q.getReviewStatus() == null || q.getReviewStatus().isBlank()) {
            q.setReviewStatus("APPROVED");
        }
        if (q.getDeleted() == null) {
            q.setDeleted(0);
        }
        if (q.getCreatorId() == null) {
            q.setCreatorId(creatorId);
        }
        if (q.getCreatedAt() == null) {
            q.setCreatedAt(LocalDateTime.now());
        }
        if (q.getId() == null) {
            questionMapper.insert(q);
        } else {
            questionMapper.updateById(q);
            optionMapper.delete(new LambdaQueryWrapper<QuestionOption>().eq(QuestionOption::getQuestionId, q.getId()));
        }
        if (isObjective(q.getType()) && (dto.options() == null || dto.options().isEmpty())) {
            throw new IllegalArgumentException("客观题必须维护选项");
        }
        if (dto.options() != null && isObjective(q.getType())) {
            for (QuestionOption option : dto.options()) {
                option.setId(null);
                option.setQuestionId(q.getId());
                optionMapper.insert(option);
            }
        }
        return detail(q.getId());
    }

    public void delete(Long id) {
        Question question = questionMapper.selectById(id);
        if (question != null) {
            question.setDeleted(1);
            questionMapper.updateById(question);
        }
    }

    public void review(Long id, String status) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        question.setReviewStatus(List.of("DRAFT", "APPROVED", "REJECTED").contains(status) ? status : "APPROVED");
        questionMapper.updateById(question);
    }

    @Transactional
    public Map<String, Object> importExcel(MultipartFile file, Long creatorId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择 Excel 文件");
        }
        int imported = 0;
        int skipped = 0;
        List<String> messages = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Long courseId = parseLong(cell(row, 0));
                String type = normalizeType(cell(row, 1));
                String stem = cell(row, 2);
                if (courseId == null || stem.isBlank()) {
                    skipped++;
                    messages.add("第 " + (i + 1) + " 行缺少课程ID或题干");
                    continue;
                }
                long exists = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                        .eq(Question::getCourseId, courseId)
                        .eq(Question::getStem, stem)
                        .eq(Question::getDeleted, 0));
                if (exists > 0) {
                    skipped++;
                    messages.add("第 " + (i + 1) + " 行题干重复，已跳过");
                    continue;
                }
                Question q = new Question();
                q.setCourseId(courseId);
                q.setCreatorId(creatorId);
                q.setType(type);
                q.setStem(stem);
                q.setCorrectAnswer(cell(row, 7));
                q.setAnalysis(cell(row, 8));
                q.setDifficulty(parseInt(cell(row, 9), 1));
                q.setKnowledgePoint(cell(row, 10));
                q.setReviewStatus("APPROVED");
                q.setDeleted(0);
                q.setCreatedAt(LocalDateTime.now());
                questionMapper.insert(q);
                if (isObjective(type)) {
                    List<QuestionOption> options = buildOptions(type, row);
                    for (QuestionOption option : options) {
                        option.setQuestionId(q.getId());
                        optionMapper.insert(option);
                    }
                }
                imported++;
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Excel导入失败：" + ex.getMessage());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", imported);
        result.put("skipped", skipped);
        result.put("messages", messages);
        return result;
    }

    public List<Map<String, Object>> duplicates(User current) {
        List<Question> all = list(null, current);
        return all.stream()
                .collect(Collectors.groupingBy(q -> q.getCourseId() + "::" + q.getStem(), LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .filter(items -> items.size() > 1)
                .map(items -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("stem", items.get(0).getStem());
                    row.put("courseId", items.get(0).getCourseId());
                    row.put("ids", items.stream().map(Question::getId).toList());
                    row.put("count", items.size());
                    return row;
                })
                .toList();
    }

    public static boolean isObjective(String type) {
        return OBJECTIVE_TYPES.contains(type);
    }

    private String normalizeType(String type) {
        if (List.of("SINGLE", "MULTIPLE", "JUDGE", "SHORT", "PROGRAM").contains(type)) {
            return type;
        }
        return "SINGLE";
    }

    private List<QuestionOption> buildOptions(String type, Row row) {
        List<QuestionOption> options = new ArrayList<>();
        if ("JUDGE".equals(type)) {
            options.add(option("A", valueOrDefault(cell(row, 3), "正确")));
            options.add(option("B", valueOrDefault(cell(row, 4), "错误")));
            return options;
        }
        for (int i = 0; i < 4; i++) {
            String text = cell(row, 3 + i);
            if (!text.isBlank()) {
                options.add(option(String.valueOf((char) ('A' + i)), text));
            }
        }
        return options;
    }

    private QuestionOption option(String key, String text) {
        QuestionOption option = new QuestionOption();
        option.setOptionKey(key);
        option.setOptionText(text);
        return option;
    }

    private String cell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                double value = cell.getNumericCellValue();
                yield value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
        };
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
