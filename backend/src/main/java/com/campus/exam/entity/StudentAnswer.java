package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student_answer")
public class StudentAnswer {
    private Long id;
    private Long attemptId;
    private Long questionId;
    private String answer;
    private Integer correct;
    private Integer score;
    private Integer manualScore;
    private String teacherComment;
    private String reviewStatus;
}
