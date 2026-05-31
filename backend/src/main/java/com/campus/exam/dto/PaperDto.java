package com.campus.exam.dto;

import com.campus.exam.entity.Paper;

import java.util.List;

public record PaperDto(Paper paper, List<PaperQuestionRequest> questions) {
}
