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
  SELECT 'ADMIN', 'admin:logs' UNION ALL
  SELECT 'ADMIN', 'admin:backup' UNION ALL
  SELECT 'TEACHER', 'teacher:manual' UNION ALL
  SELECT 'TEACHER', 'teacher:analysis' UNION ALL
  SELECT 'TEACHER', 'teacher:monitor' UNION ALL
  SELECT 'TEACHER', 'teacher:appeals' UNION ALL
  SELECT 'STUDENT', 'student:advice' UNION ALL
  SELECT 'STUDENT', 'student:appeals'
) seed
WHERE NOT EXISTS (
  SELECT 1 FROM role_permission
  WHERE role = seed.role_value AND permission_code = seed.code_value
);

DROP PROCEDURE IF EXISTS add_column_if_missing;
