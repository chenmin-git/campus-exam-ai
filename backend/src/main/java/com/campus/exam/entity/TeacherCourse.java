package com.campus.exam.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("teacher_course")
public class TeacherCourse {
    private Long id;
    private Long teacherId;
    private Long courseId;
    private Long classId;
}
