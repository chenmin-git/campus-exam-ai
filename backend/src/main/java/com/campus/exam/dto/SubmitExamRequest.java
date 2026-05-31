package com.campus.exam.dto;

import java.util.List;

public record SubmitExamRequest(List<AnswerItem> answers) {
    public record AnswerItem(Long questionId, String answer) {
    }
}
