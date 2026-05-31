package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("paper_question")
public class PaperQuestion {
    private Long id;
    private Long paperId;
    private Long questionId;
    private Integer score;
    private Integer sortNo;
}
