USE campus_exam_ai;

DELIMITER //
DROP PROCEDURE IF EXISTS add_column_if_missing//
CREATE PROCEDURE add_column_if_missing(IN table_name_value VARCHAR(80), IN column_name_value VARCHAR(80), IN ddl_value TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name_value
      AND COLUMN_NAME = column_name_value
  ) THEN
    SET @ddl = CONCAT('ALTER TABLE ', table_name_value, ' ADD COLUMN ', ddl_value);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_column_if_missing('question', 'knowledge_point', 'knowledge_point VARCHAR(120)');
CALL add_column_if_missing('question', 'review_status', 'review_status VARCHAR(20) DEFAULT ''APPROVED''');
CALL add_column_if_missing('question', 'deleted', 'deleted TINYINT DEFAULT 0');
CALL add_column_if_missing('paper', 'allow_retake', 'allow_retake TINYINT DEFAULT 0');
CALL add_column_if_missing('paper', 'start_time', 'start_time DATETIME NULL');
CALL add_column_if_missing('paper', 'end_time', 'end_time DATETIME NULL');
CALL add_column_if_missing('exam_attempt', 'objective_score', 'objective_score INT DEFAULT 0');
CALL add_column_if_missing('exam_attempt', 'subjective_score', 'subjective_score INT DEFAULT 0');
CALL add_column_if_missing('exam_attempt', 'review_status', 'review_status VARCHAR(20) DEFAULT ''NONE''');
CALL add_column_if_missing('exam_attempt', 'review_comment', 'review_comment VARCHAR(500)');
CALL add_column_if_missing('student_answer', 'manual_score', 'manual_score INT DEFAULT 0');
CALL add_column_if_missing('student_answer', 'teacher_comment', 'teacher_comment VARCHAR(500)');
CALL add_column_if_missing('student_answer', 'review_status', 'review_status VARCHAR(20) DEFAULT ''AUTO''');

ALTER TABLE student_answer MODIFY COLUMN answer TEXT;

CREATE TABLE IF NOT EXISTS exam_monitor_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  paper_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  detail VARCHAR(500),
  ip VARCHAR(80),
  user_agent VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS grade_appeal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  reason VARCHAR(800) NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING',
  reply VARCHAR(800),
  reviewer_id BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  reviewed_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  role VARCHAR(20),
  title VARCHAR(120) NOT NULL,
  content VARCHAR(800) NOT NULL,
  read_flag TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(80),
  role VARCHAR(20),
  action VARCHAR(120) NOT NULL,
  target VARCHAR(160),
  detail VARCHAR(1000),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS backup_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(160) NOT NULL,
  file_path VARCHAR(500),
  status VARCHAR(20) DEFAULT 'CREATED',
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO permission (code, name, module, path, sort_no)
SELECT 'teacher:manual', '人工阅卷', '教学管理', '/teacher/manual', 33
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'teacher:manual');
INSERT INTO permission (code, name, module, path, sort_no)
SELECT 'teacher:analysis', '成绩分析', '教学管理', '/teacher/analysis', 34
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'teacher:analysis');
INSERT INTO permission (code, name, module, path, sort_no)
SELECT 'teacher:monitor', '考试监控', '教学管理', '/teacher/monitor', 35
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'teacher:monitor');
INSERT INTO permission (code, name, module, path, sort_no)
SELECT 'teacher:appeals', '成绩复查', '教学管理', '/teacher/appeals', 36
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'teacher:appeals');
INSERT INTO permission (code, name, module, path, sort_no)
SELECT 'student:advice', '学习建议', '学生中心', '/student/advice', 53
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'student:advice');
INSERT INTO permission (code, name, module, path, sort_no)
SELECT 'student:appeals', '成绩申诉', '学生中心', '/student/appeals', 54
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'student:appeals');
INSERT INTO permission (code, name, module, path, sort_no)
SELECT 'student:notifications', '通知中心', '学生中心', '/student/notifications', 55
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'student:notifications');
INSERT INTO permission (code, name, module, path, sort_no)
SELECT 'admin:logs', '操作日志', '系统管理', '/admin/logs', 80
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'admin:logs');
INSERT INTO permission (code, name, module, path, sort_no)
SELECT 'admin:backup', '备份恢复', '系统管理', '/admin/backup', 81
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'admin:backup');

INSERT INTO role_permission (role, permission_code)
SELECT role_value, code_value
FROM (
  SELECT 'ADMIN' role_value, 'teacher:manual' code_value UNION ALL
  SELECT 'ADMIN', 'teacher:analysis' UNION ALL
  SELECT 'ADMIN', 'teacher:monitor' UNION ALL
  SELECT 'ADMIN', 'teacher:appeals' UNION ALL
  SELECT 'ADMIN', 'student:advice' UNION ALL
  SELECT 'ADMIN', 'student:appeals' UNION ALL
  SELECT 'ADMIN', 'student:notifications' UNION ALL
  SELECT 'ADMIN', 'admin:logs' UNION ALL
  SELECT 'ADMIN', 'admin:backup' UNION ALL
  SELECT 'TEACHER', 'teacher:manual' UNION ALL
  SELECT 'TEACHER', 'teacher:analysis' UNION ALL
  SELECT 'TEACHER', 'teacher:monitor' UNION ALL
  SELECT 'TEACHER', 'teacher:appeals' UNION ALL
  SELECT 'STUDENT', 'student:advice' UNION ALL
  SELECT 'STUDENT', 'student:appeals' UNION ALL
  SELECT 'STUDENT', 'student:notifications'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM role_permission
  WHERE role = seed.role_value AND permission_code = seed.code_value
);

UPDATE question
SET knowledge_point = CASE id
  WHEN 1 THEN 'Java继承'
  WHEN 2 THEN '集合框架'
  WHEN 3 THEN 'SpringBoot基础'
  WHEN 4 THEN 'SQL查询'
  WHEN 5 THEN 'ArrayList底层结构'
  WHEN 6 THEN 'SpringBoot配置'
  WHEN 7 THEN '事务ACID'
  WHEN 8 THEN '主键索引'
  WHEN 9 THEN 'Vue响应式'
  WHEN 10 THEN 'Axios拦截器'
  WHEN 11 THEN '需求分析'
  WHEN 12 THEN '软件测试'
  ELSE knowledge_point
END
WHERE id BETWEEN 1 AND 12 AND (knowledge_point IS NULL OR knowledge_point = '');

INSERT INTO question (course_id, creator_id, type, stem, correct_answer, analysis, knowledge_point, difficulty)
SELECT 1, 2, 'SHORT', '请简述 Java 面向对象中封装、继承、多态的作用。', '围绕隐藏实现细节、复用父类能力、同一接口多种实现展开说明。', '主观题重点看概念准确性、示例完整性和语言表达。', '面向对象基础', 3
WHERE NOT EXISTS (SELECT 1 FROM question WHERE stem = '请简述 Java 面向对象中封装、继承、多态的作用。');
INSERT INTO question (course_id, creator_id, type, stem, correct_answer, analysis, knowledge_point, difficulty)
SELECT 1, 2, 'PROGRAM', '编写一个方法，统计 List<String> 中每个单词出现的次数，并返回 Map<String, Integer>。', '可使用 HashMap 遍历累加，也可使用 Stream 分组统计。', '编程题关注边界处理、集合 API 使用和返回结果正确性。', '集合与Map', 4
WHERE NOT EXISTS (SELECT 1 FROM question WHERE stem = '编写一个方法，统计 List<String> 中每个单词出现的次数，并返回 Map<String, Integer>。');
INSERT INTO question (course_id, creator_id, type, stem, correct_answer, analysis, knowledge_point, difficulty)
SELECT 2, 2, 'SINGLE', '数据库中用于加速查询但会增加写入维护成本的结构是？', 'C', '索引可以提升查询效率，但插入、更新、删除时需要维护索引。', '索引基础', 2
WHERE NOT EXISTS (SELECT 1 FROM question WHERE stem = '数据库中用于加速查询但会增加写入维护成本的结构是？');
INSERT INTO question (course_id, creator_id, type, stem, correct_answer, analysis, knowledge_point, difficulty)
SELECT 3, 2, 'MULTIPLE', '以下哪些属于 Vue 常见内置指令？', 'A,B,D', 'v-if、v-for、v-model 都是 Vue 常用内置指令。', 'Vue指令', 2
WHERE NOT EXISTS (SELECT 1 FROM question WHERE stem = '以下哪些属于 Vue 常见内置指令？');

SET @q_index = (SELECT id FROM question WHERE stem = '数据库中用于加速查询但会增加写入维护成本的结构是？' LIMIT 1);
SET @q_vue_directive = (SELECT id FROM question WHERE stem = '以下哪些属于 Vue 常见内置指令？' LIMIT 1);

INSERT INTO question_option (question_id, option_key, option_text)
SELECT seed.question_id, seed.option_key, seed.option_text
FROM (
  SELECT @q_index question_id, 'A' option_key, '视图' option_text UNION ALL
  SELECT @q_index, 'B', '触发器' UNION ALL
  SELECT @q_index, 'C', '索引' UNION ALL
  SELECT @q_index, 'D', '存储过程' UNION ALL
  SELECT @q_vue_directive, 'A', 'v-if' UNION ALL
  SELECT @q_vue_directive, 'B', 'v-for' UNION ALL
  SELECT @q_vue_directive, 'C', 'v-request' UNION ALL
  SELECT @q_vue_directive, 'D', 'v-model'
) seed
WHERE seed.question_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM question_option qo
    WHERE qo.question_id = seed.question_id AND qo.option_key = seed.option_key
  );

INSERT INTO paper (course_id, creator_id, title, duration_minutes, total_score, published, allow_retake, start_time, end_time)
SELECT 1, 2, 'Java综合主观题模拟', 60, 30, 1, 1, '2026-05-20 08:00:00', '2026-06-30 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM paper WHERE title = 'Java综合主观题模拟');
INSERT INTO paper (course_id, creator_id, title, duration_minutes, total_score, published, allow_retake, start_time, end_time)
SELECT 2, 2, '数据库索引与事务强化', 45, 40, 1, 0, '2026-05-20 08:00:00', '2026-06-30 23:59:59'
WHERE NOT EXISTS (SELECT 1 FROM paper WHERE title = '数据库索引与事务强化');

SET @paper_java_subjective = (SELECT id FROM paper WHERE title = 'Java综合主观题模拟' LIMIT 1);
SET @paper_db_index = (SELECT id FROM paper WHERE title = '数据库索引与事务强化' LIMIT 1);
SET @q_java_extends = (SELECT id FROM question WHERE stem = 'Java 中用于定义类继承关系的关键字是？' LIMIT 1);
SET @q_java_oop = (SELECT id FROM question WHERE stem = '请简述 Java 面向对象中封装、继承、多态的作用。' LIMIT 1);
SET @q_java_program = (SELECT id FROM question WHERE stem = '编写一个方法，统计 List<String> 中每个单词出现的次数，并返回 Map<String, Integer>。' LIMIT 1);
SET @q_sql_select = (SELECT id FROM question WHERE stem = 'SQL 中用于查询数据的关键字是？' LIMIT 1);
SET @q_acid = (SELECT id FROM question WHERE stem = '数据库事务 ACID 特性包括哪些？' LIMIT 1);
SET @q_pk = (SELECT id FROM question WHERE stem = '主键索引可以唯一标识表中的一行记录。' LIMIT 1);

INSERT INTO paper_question (paper_id, question_id, score, sort_no)
SELECT seed.paper_id, seed.question_id, seed.score, seed.sort_no
FROM (
  SELECT @paper_java_subjective paper_id, @q_java_extends question_id, 10 score, 1 sort_no UNION ALL
  SELECT @paper_java_subjective, @q_java_oop, 10, 2 UNION ALL
  SELECT @paper_java_subjective, @q_java_program, 10, 3 UNION ALL
  SELECT @paper_db_index, @q_sql_select, 10, 1 UNION ALL
  SELECT @paper_db_index, @q_acid, 10, 2 UNION ALL
  SELECT @paper_db_index, @q_pk, 10, 3 UNION ALL
  SELECT @paper_db_index, @q_index, 10, 4
) seed
WHERE seed.paper_id IS NOT NULL AND seed.question_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM paper_question pq
    WHERE pq.paper_id = seed.paper_id AND pq.question_id = seed.question_id
  );

INSERT INTO exam_attempt (paper_id, student_id, score, objective_score, subjective_score, status, review_status, started_at, submitted_at, due_at)
SELECT @paper_java_subjective, 3, 10, 10, 0, 'PENDING_REVIEW', 'PENDING', '2026-05-28 09:00:00', '2026-05-28 09:48:00', '2026-05-28 10:00:00'
WHERE @paper_java_subjective IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM exam_attempt WHERE paper_id = @paper_java_subjective AND student_id = 3 AND started_at = '2026-05-28 09:00:00');
INSERT INTO exam_attempt (paper_id, student_id, score, objective_score, subjective_score, status, review_status, started_at, submitted_at, due_at)
SELECT @paper_java_subjective, 5, 26, 10, 16, 'SUBMITTED', 'DONE', '2026-05-28 09:05:00', '2026-05-28 09:52:00', '2026-05-28 10:05:00'
WHERE @paper_java_subjective IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM exam_attempt WHERE paper_id = @paper_java_subjective AND student_id = 5 AND started_at = '2026-05-28 09:05:00');
INSERT INTO exam_attempt (paper_id, student_id, score, objective_score, subjective_score, status, review_status, started_at, submitted_at, due_at)
SELECT @paper_db_index, 6, 30, 30, 0, 'SUBMITTED', 'DONE', '2026-05-29 14:00:00', '2026-05-29 14:36:00', '2026-05-29 14:45:00'
WHERE @paper_db_index IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM exam_attempt WHERE paper_id = @paper_db_index AND student_id = 6 AND started_at = '2026-05-29 14:00:00');

SET @attempt_java_pending = (SELECT id FROM exam_attempt WHERE paper_id = @paper_java_subjective AND student_id = 3 AND started_at = '2026-05-28 09:00:00' LIMIT 1);
SET @attempt_java_reviewed = (SELECT id FROM exam_attempt WHERE paper_id = @paper_java_subjective AND student_id = 5 AND started_at = '2026-05-28 09:05:00' LIMIT 1);
SET @attempt_db_reviewed = (SELECT id FROM exam_attempt WHERE paper_id = @paper_db_index AND student_id = 6 AND started_at = '2026-05-29 14:00:00' LIMIT 1);

INSERT INTO student_answer (attempt_id, question_id, answer, correct, score, manual_score, teacher_comment, review_status)
SELECT seed.attempt_id, seed.question_id, seed.answer, seed.correct, seed.score, seed.manual_score, seed.teacher_comment, seed.review_status
FROM (
  SELECT @attempt_java_pending attempt_id, @q_java_extends question_id, 'B' answer, 1 correct, 10 score, 10 manual_score, NULL teacher_comment, 'AUTO' review_status UNION ALL
  SELECT @attempt_java_pending, @q_java_oop, '封装隐藏细节，继承复用代码，多态让同一方法有不同实现。', 0, 0, 0, NULL, 'PENDING' UNION ALL
  SELECT @attempt_java_pending, @q_java_program, '使用 Map 遍历 List，并用 getOrDefault 累加。', 0, 0, 0, NULL, 'PENDING' UNION ALL
  SELECT @attempt_java_reviewed, @q_java_extends, 'B', 1, 10, 10, NULL, 'AUTO' UNION ALL
  SELECT @attempt_java_reviewed, @q_java_oop, '封装保护内部状态，继承复用父类能力，多态支持面向接口编程。', 0, 8, 8, '概念清楚，示例略少。', 'REVIEWED' UNION ALL
  SELECT @attempt_java_reviewed, @q_java_program, 'public Map<String,Integer> count(List<String> words){ Map<String,Integer> map=new HashMap<>(); for(String w:words){ map.put(w,map.getOrDefault(w,0)+1); } return map; }', 0, 8, 8, '思路正确，建议补充空值处理。', 'REVIEWED' UNION ALL
  SELECT @attempt_db_reviewed, @q_sql_select, 'A', 1, 10, 10, NULL, 'AUTO' UNION ALL
  SELECT @attempt_db_reviewed, @q_acid, 'A,B,C,D', 1, 10, 10, NULL, 'AUTO' UNION ALL
  SELECT @attempt_db_reviewed, @q_pk, 'A', 1, 10, 10, NULL, 'AUTO' UNION ALL
  SELECT @attempt_db_reviewed, @q_index, 'B', 0, 0, 0, NULL, 'AUTO'
) seed
WHERE seed.attempt_id IS NOT NULL AND seed.question_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM student_answer sa
    WHERE sa.attempt_id = seed.attempt_id AND sa.question_id = seed.question_id
  );

INSERT INTO exam_monitor_event (attempt_id, paper_id, student_id, event_type, detail, ip, user_agent, created_at)
SELECT seed.attempt_id, seed.paper_id, seed.student_id, seed.event_type, seed.detail, seed.ip, seed.user_agent, seed.created_at
FROM (
  SELECT @attempt_java_pending attempt_id, @paper_java_subjective paper_id, 3 student_id, 'FULLSCREEN_EXIT' event_type, '学生考试中退出全屏' detail, '192.168.1.23' ip, 'Chrome / macOS' user_agent, '2026-05-28 09:20:00' created_at UNION ALL
  SELECT @attempt_java_pending, @paper_java_subjective, 3, 'BLUR', '窗口失焦 1 次', '192.168.1.23', 'Chrome / macOS', '2026-05-28 09:31:00' UNION ALL
  SELECT @attempt_db_reviewed, @paper_db_index, 6, 'VISIBILITY_CHANGE', '切换到后台查看资料', '192.168.1.42', 'Edge / Windows', '2026-05-29 14:18:00'
) seed
WHERE seed.attempt_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM exam_monitor_event e
    WHERE e.attempt_id = seed.attempt_id AND e.event_type = seed.event_type AND e.created_at = seed.created_at
  );

INSERT INTO grade_appeal (attempt_id, student_id, reason, status, reply, reviewer_id, created_at, reviewed_at)
SELECT @attempt_java_reviewed, 5, '申请复查编程题边界条件给分。', 'PENDING', NULL, NULL, '2026-05-29 16:10:00', NULL
WHERE @attempt_java_reviewed IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM grade_appeal WHERE attempt_id = @attempt_java_reviewed AND student_id = 5);

INSERT INTO notification (user_id, role, title, content, read_flag, created_at)
SELECT seed.user_id, seed.role, seed.title, seed.content, seed.read_flag, seed.created_at
FROM (
  SELECT 3 user_id, NULL role, '主观题等待阅卷' title, '你提交的《Java综合主观题模拟》包含主观题，请等待教师批改。' content, 0 read_flag, '2026-05-28 09:49:00' created_at UNION ALL
  SELECT 5, NULL, '成绩申诉已提交', '你的成绩复查申请已提交，教师会尽快处理。', 0, '2026-05-29 16:11:00' UNION ALL
  SELECT NULL, 'STUDENT', '数据库强化练习发布', '数据库索引与事务强化试卷已发布，请相关班级同学按时完成。', 0, '2026-05-29 08:30:00'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM notification n
  WHERE n.title = seed.title AND n.created_at = seed.created_at
);

INSERT INTO operation_log (user_id, username, role, action, target, detail, created_at)
SELECT seed.user_id, seed.username, seed.role, seed.action, seed.target, seed.detail, seed.created_at
FROM (
  SELECT 1 user_id, 'admin' username, 'ADMIN' role, '保存角色权限' action, 'TEACHER' target, '授予教师人工阅卷、成绩分析、考试监控权限' detail, '2026-05-28 08:30:00' created_at UNION ALL
  SELECT 2, 'teacher', 'TEACHER', '保存试卷', 'paper:Java综合主观题模拟', '发布包含简答题和编程题的综合练习', '2026-05-28 08:45:00' UNION ALL
  SELECT 3, 'student', 'STUDENT', '提交考试', 'paper:Java综合主观题模拟', '客观题自动得分，主观题进入待阅卷', '2026-05-28 09:48:00'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM operation_log l
  WHERE l.action = seed.action AND l.target = seed.target AND l.created_at = seed.created_at
);

INSERT INTO backup_record (name, file_path, status, remark, created_at)
SELECT seed.name, seed.file_path, seed.status, seed.remark, seed.created_at
FROM (
  SELECT '开源演示数据备份' name, 'backup/campus_exam_ai_demo.sql' file_path, 'CREATED' status, '包含题库、试卷、成绩、监控、申诉与通知示例数据' remark, '2026-05-29 18:00:00' created_at UNION ALL
  SELECT '发布前数据快照', 'backup/campus_exam_ai_release.sql', 'RESTORED', '用于演示恢复流程的逻辑记录', '2026-05-30 10:00:00'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM backup_record b
  WHERE b.name = seed.name AND b.created_at = seed.created_at
);

DROP PROCEDURE IF EXISTS add_column_if_missing;
