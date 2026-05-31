package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_option")
public class QuestionOption {
    private Long id;
    private Long questionId;
    private String optionKey;
    private String optionText;
}
