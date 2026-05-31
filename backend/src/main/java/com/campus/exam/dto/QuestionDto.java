package com.campus.exam.dto;

import com.campus.exam.entity.Question;
import com.campus.exam.entity.QuestionOption;

import java.util.List;

public record QuestionDto(Question question, List<QuestionOption> options) {
}
