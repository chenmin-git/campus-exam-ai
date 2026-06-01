-- Campus Exam AI complete database initialization script.
-- This script recreates the campus_exam_ai database and seeds demo data.
-- Run with: mysql -uroot -proot < sql/campus_exam_ai.sql

DROP DATABASE IF EXISTS campus_exam_ai;
CREATE DATABASE campus_exam_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE campus_exam_ai;

CREATE TABLE class_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  grade VARCHAR(20),
  major VARCHAR(80)
);

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(80) NOT NULL,
  role VARCHAR(20) NOT NULL,
  class_id BIGINT,
  phone VARCHAR(30),
  email VARCHAR(120),
  enabled TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(50),
  description VARCHAR(500)
);

CREATE TABLE teacher_course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL
);

CREATE TABLE question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  creator_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  stem TEXT NOT NULL,
  correct_answer VARCHAR(100) NOT NULL,
  analysis TEXT,
  knowledge_point VARCHAR(120),
  review_status VARCHAR(20) DEFAULT 'APPROVED',
  deleted TINYINT DEFAULT 0,
  difficulty INT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE question_option (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  option_key VARCHAR(10) NOT NULL,
  option_text VARCHAR(1000) NOT NULL
);

CREATE TABLE paper (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  creator_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  duration_minutes INT NOT NULL,
  total_score INT DEFAULT 0,
  published TINYINT DEFAULT 0,
  allow_retake TINYINT DEFAULT 0,
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE paper_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  score INT NOT NULL,
  sort_no INT DEFAULT 1
);

CREATE TABLE exam_attempt (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  paper_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  score INT DEFAULT 0,
  objective_score INT DEFAULT 0,
  subjective_score INT DEFAULT 0,
  status VARCHAR(20) NOT NULL,
  review_status VARCHAR(20) DEFAULT 'NONE',
  review_comment VARCHAR(500),
  started_at DATETIME,
  submitted_at DATETIME,
  due_at DATETIME
);

CREATE TABLE student_answer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  answer TEXT,
  correct TINYINT DEFAULT 0,
  score INT DEFAULT 0,
  manual_score INT DEFAULT 0,
  teacher_comment VARCHAR(500),
  review_status VARCHAR(20) DEFAULT 'AUTO'
);

CREATE TABLE exam_monitor_event (
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

CREATE TABLE grade_appeal (
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

CREATE TABLE notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  role VARCHAR(20),
  title VARCHAR(120) NOT NULL,
  content VARCHAR(800) NOT NULL,
  read_flag TINYINT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(80),
  role VARCHAR(20),
  action VARCHAR(120) NOT NULL,
  target VARCHAR(160),
  detail VARCHAR(1000),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE backup_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(160) NOT NULL,
  file_path VARCHAR(500),
  status VARCHAR(20) DEFAULT 'CREATED',
  remark VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_analysis_cache (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  question_id BIGINT NOT NULL,
  student_answer VARCHAR(200),
  content TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  creator_id BIGINT NOT NULL,
  enabled TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(80) NOT NULL UNIQUE,
  name VARCHAR(80) NOT NULL,
  module VARCHAR(80) NOT NULL,
  path VARCHAR(160) NOT NULL,
  sort_no INT DEFAULT 1
);

CREATE TABLE role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role VARCHAR(20) NOT NULL,
  permission_code VARCHAR(80) NOT NULL
);

INSERT INTO class_info (id, name, grade, major) VALUES
(1, '计科2301班', '2023', '计算机科学与技术'),
(2, '软工2301班', '2023', '软件工程'),
(3, '计科2202班', '2022', '计算机科学与技术'),
(4, '数媒2301班', '2023', '数字媒体技术');

INSERT INTO sys_user (id, username, password, real_name, role, class_id, phone, email) VALUES
(1, 'admin', '123456', '系统管理员', 'ADMIN', NULL, '13800000000', 'admin@example.com'),
(2, 'teacher', '123456', '张老师', 'TEACHER', NULL, '13800000001', 'teacher@example.com'),
(3, 'student', '123456', '李同学', 'STUDENT', 1, '13800000002', 'student@example.com'),
(4, 'teacher2', '123456', '王老师', 'TEACHER', NULL, '13800000003', 'wang@example.com'),
(5, 'student2', '123456', '赵同学', 'STUDENT', 1, '13800000004', 'zhao@example.com'),
(6, 'student3', '123456', '钱同学', 'STUDENT', 2, '13800000005', 'qian@example.com'),
(7, 'student4', '123456', '孙同学', 'STUDENT', 3, '13800000006', 'sun@example.com'),
(8, 'student5', '123456', '周同学', 'STUDENT', 4, '13800000007', 'zhou@example.com');

INSERT INTO course (id, name, code, description) VALUES
(1, 'Java程序设计', 'JAVA101', '面向对象、集合、异常与基础 Web 开发'),
(2, '数据库原理', 'DB101', '关系模型、SQL、事务与索引基础'),
(3, 'Web前端开发', 'WEB101', 'Vue、组件化、Axios 与前端工程化'),
(4, '软件工程', 'SE101', '需求分析、系统设计、测试与项目管理');

INSERT INTO teacher_course (teacher_id, course_id, class_id) VALUES
(2, 1, 1),
(2, 2, 1),
(2, 3, 2),
(4, 4, 3),
(4, 1, 4);

INSERT INTO question (id, course_id, creator_id, type, stem, correct_answer, analysis, difficulty) VALUES
(1, 1, 2, 'SINGLE', 'Java 中用于定义类继承关系的关键字是？', 'B', 'extends 用于类继承，implements 用于接口实现。', 1),
(2, 1, 2, 'MULTIPLE', '以下哪些属于 Java 集合框架中的接口？', 'A,B,D', 'List、Set、Map 都是集合框架中的重要接口。', 2),
(3, 1, 2, 'JUDGE', 'Spring Boot 可以通过内嵌服务器启动 Web 应用。', 'A', 'Spring Boot 默认可使用内嵌 Tomcat 启动 Web 应用。', 1),
(4, 2, 2, 'SINGLE', 'SQL 中用于查询数据的关键字是？', 'A', 'SELECT 用于查询数据。', 1),
(5, 1, 2, 'SINGLE', 'Java 中 ArrayList 底层主要基于哪种结构实现？', 'A', 'ArrayList 底层使用动态数组，适合随机访问。', 2),
(6, 1, 2, 'MULTIPLE', '以下哪些是 Spring Boot 常用配置文件格式？', 'A,C', 'Spring Boot 常用 application.yml 与 application.properties。', 2),
(7, 2, 2, 'MULTIPLE', '数据库事务 ACID 特性包括哪些？', 'A,B,C,D', 'ACID 包括原子性、一致性、隔离性、持久性。', 3),
(8, 2, 2, 'JUDGE', '主键索引可以唯一标识表中的一行记录。', 'A', '主键具有唯一性和非空性。', 1),
(9, 3, 2, 'SINGLE', 'Vue3 中用于创建响应式引用的 API 是？', 'B', 'ref 用于创建响应式引用。', 2),
(10, 3, 2, 'MULTIPLE', 'Axios 请求拦截器常用于处理哪些内容？', 'A,B,C', '请求拦截器可处理 token、请求头和统一参数。', 3),
(11, 4, 4, 'SINGLE', '软件需求规格说明书通常对应哪个阶段产物？', 'A', 'SRS 是需求分析阶段的重要产物。', 2),
(12, 4, 4, 'JUDGE', '黑盒测试主要关注程序内部代码结构。', 'B', '黑盒测试关注输入输出和功能行为。', 2);

INSERT INTO question (id, course_id, creator_id, type, stem, correct_answer, analysis, knowledge_point, difficulty) VALUES
(13, 1, 2, 'SHORT', '请简述 Java 面向对象中封装、继承、多态的作用。', '围绕隐藏实现细节、复用父类能力、同一接口多种实现展开说明。', '主观题重点看概念准确性、示例完整性和语言表达。', '面向对象基础', 3),
(14, 1, 2, 'PROGRAM', '编写一个方法，统计 List<String> 中每个单词出现的次数，并返回 Map<String, Integer>。', '可使用 HashMap 遍历累加，也可使用 Stream 分组统计。', '编程题关注边界处理、集合 API 使用和返回结果正确性。', '集合与Map', 4),
(15, 2, 2, 'SINGLE', '数据库中用于加速查询但会增加写入维护成本的结构是？', 'C', '索引可以提升查询效率，但插入、更新、删除时需要维护索引。', '索引基础', 2),
(16, 3, 2, 'MULTIPLE', '以下哪些属于 Vue 常见内置指令？', 'A,B,D', 'v-if、v-for、v-model 都是 Vue 常用内置指令。', 'Vue指令', 2);

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
WHERE id BETWEEN 1 AND 12;

INSERT INTO question_option (question_id, option_key, option_text) VALUES
(1, 'A', 'implements'),
(1, 'B', 'extends'),
(1, 'C', 'import'),
(1, 'D', 'package'),
(2, 'A', 'List'),
(2, 'B', 'Set'),
(2, 'C', 'Thread'),
(2, 'D', 'Map'),
(3, 'A', '正确'),
(3, 'B', '错误'),
(4, 'A', 'SELECT'),
(4, 'B', 'UPDATE'),
(4, 'C', 'DELETE'),
(4, 'D', 'INSERT'),
(5, 'A', '动态数组'),
(5, 'B', '链表'),
(5, 'C', '哈希表'),
(5, 'D', '栈'),
(6, 'A', 'application.yml'),
(6, 'B', 'web.xml'),
(6, 'C', 'application.properties'),
(6, 'D', 'package.json'),
(7, 'A', '原子性'),
(7, 'B', '一致性'),
(7, 'C', '隔离性'),
(7, 'D', '持久性'),
(8, 'A', '正确'),
(8, 'B', '错误'),
(9, 'A', 'reactiveOnly'),
(9, 'B', 'ref'),
(9, 'C', 'mounted'),
(9, 'D', 'watcher'),
(10, 'A', '携带 token'),
(10, 'B', '设置请求头'),
(10, 'C', '统一处理参数'),
(10, 'D', '直接渲染页面'),
(11, 'A', '需求分析'),
(11, 'B', '编码实现'),
(11, 'C', '部署上线'),
(11, 'D', '运维监控'),
(12, 'A', '正确'),
(12, 'B', '错误');

INSERT INTO question_option (question_id, option_key, option_text) VALUES
(15, 'A', '视图'),
(15, 'B', '触发器'),
(15, 'C', '索引'),
(15, 'D', '存储过程'),
(16, 'A', 'v-if'),
(16, 'B', 'v-for'),
(16, 'C', 'v-request'),
(16, 'D', 'v-model');

INSERT INTO paper (id, course_id, creator_id, title, duration_minutes, total_score, published) VALUES
(1, 1, 2, 'Java程序设计阶段测试', 45, 30, 1),
(2, 2, 2, '数据库原理单元测验', 40, 30, 1),
(3, 3, 2, 'Web前端开发课堂练习', 35, 20, 1),
(4, 4, 4, '软件工程基础测试', 30, 20, 0);

INSERT INTO paper (id, course_id, creator_id, title, duration_minutes, total_score, published, allow_retake, start_time, end_time) VALUES
(5, 1, 2, 'Java综合主观题模拟', 60, 30, 1, 1, '2026-05-20 08:00:00', '2026-06-30 23:59:59'),
(6, 2, 2, '数据库索引与事务强化', 45, 40, 1, 0, '2026-05-20 08:00:00', '2026-06-30 23:59:59');

INSERT INTO paper_question (paper_id, question_id, score, sort_no) VALUES
(1, 1, 10, 1),
(1, 2, 10, 2),
(1, 3, 10, 3),
(2, 4, 10, 1),
(2, 7, 10, 2),
(2, 8, 10, 3),
(3, 9, 10, 1),
(3, 10, 10, 2),
(4, 11, 10, 1),
(4, 12, 10, 2),
(5, 1, 10, 1),
(5, 13, 10, 2),
(5, 14, 10, 3),
(6, 4, 10, 1),
(6, 7, 10, 2),
(6, 8, 10, 3),
(6, 15, 10, 4);

INSERT INTO exam_attempt (id, paper_id, student_id, score, status, started_at, submitted_at, due_at) VALUES
(1, 1, 3, 20, 'SUBMITTED', '2026-05-25 09:00:00', '2026-05-25 09:35:00', '2026-05-25 09:45:00'),
(2, 1, 5, 30, 'SUBMITTED', '2026-05-25 09:02:00', '2026-05-25 09:32:00', '2026-05-25 09:47:00'),
(3, 2, 6, 20, 'SUBMITTED', '2026-05-26 14:00:00', '2026-05-26 14:29:00', '2026-05-26 14:40:00'),
(4, 3, 7, 10, 'SUBMITTED', '2026-05-27 10:00:00', '2026-05-27 10:25:00', '2026-05-27 10:35:00');

INSERT INTO exam_attempt (id, paper_id, student_id, score, objective_score, subjective_score, status, review_status, started_at, submitted_at, due_at) VALUES
(5, 5, 3, 10, 10, 0, 'PENDING_REVIEW', 'PENDING', '2026-05-28 09:00:00', '2026-05-28 09:48:00', '2026-05-28 10:00:00'),
(6, 5, 5, 26, 10, 16, 'SUBMITTED', 'DONE', '2026-05-28 09:05:00', '2026-05-28 09:52:00', '2026-05-28 10:05:00'),
(7, 6, 6, 30, 30, 0, 'SUBMITTED', 'DONE', '2026-05-29 14:00:00', '2026-05-29 14:36:00', '2026-05-29 14:45:00');

INSERT INTO student_answer (attempt_id, question_id, answer, correct, score) VALUES
(1, 1, 'B', 1, 10),
(1, 2, 'A,B', 0, 0),
(1, 3, 'A', 1, 10),
(2, 1, 'B', 1, 10),
(2, 2, 'A,B,D', 1, 10),
(2, 3, 'A', 1, 10),
(3, 4, 'A', 1, 10),
(3, 7, 'A,B,C', 0, 0),
(3, 8, 'A', 1, 10),
(4, 9, 'A', 0, 0),
(4, 10, 'A,B,C', 1, 10);

INSERT INTO student_answer (attempt_id, question_id, answer, correct, score, manual_score, teacher_comment, review_status) VALUES
(5, 1, 'B', 1, 10, 10, NULL, 'AUTO'),
(5, 13, '封装隐藏细节，继承复用代码，多态让同一方法有不同实现。', 0, 0, 0, NULL, 'PENDING'),
(5, 14, '使用 Map 遍历 List，并用 getOrDefault 累加。', 0, 0, 0, NULL, 'PENDING'),
(6, 1, 'B', 1, 10, 10, NULL, 'AUTO'),
(6, 13, '封装保护内部状态，继承复用父类能力，多态支持面向接口编程。', 0, 8, 8, '概念清楚，示例略少。', 'REVIEWED'),
(6, 14, 'public Map<String,Integer> count(List<String> words){ Map<String,Integer> map=new HashMap<>(); for(String w:words){ map.put(w,map.getOrDefault(w,0)+1); } return map; }', 0, 8, 8, '思路正确，建议补充空值处理。', 'REVIEWED'),
(7, 4, 'A', 1, 10, 10, NULL, 'AUTO'),
(7, 7, 'A,B,C,D', 1, 10, 10, NULL, 'AUTO'),
(7, 8, 'A', 1, 10, 10, NULL, 'AUTO'),
(7, 15, 'B', 0, 0, 0, NULL, 'AUTO');

INSERT INTO exam_monitor_event (attempt_id, paper_id, student_id, event_type, detail, ip, user_agent, created_at) VALUES
(5, 5, 3, 'FULLSCREEN_EXIT', '学生考试中退出全屏', '192.168.1.23', 'Chrome / macOS', '2026-05-28 09:20:00'),
(5, 5, 3, 'BLUR', '窗口失焦 1 次', '192.168.1.23', 'Chrome / macOS', '2026-05-28 09:31:00'),
(7, 6, 6, 'VISIBILITY_CHANGE', '切换到后台查看资料', '192.168.1.42', 'Edge / Windows', '2026-05-29 14:18:00');

INSERT INTO grade_appeal (attempt_id, student_id, reason, status, reply, reviewer_id, created_at, reviewed_at) VALUES
(6, 5, '申请复查编程题边界条件给分。', 'PENDING', NULL, NULL, '2026-05-29 16:10:00', NULL);

INSERT INTO notification (user_id, role, title, content, read_flag, created_at) VALUES
(3, NULL, '主观题等待阅卷', '你提交的《Java综合主观题模拟》包含主观题，请等待教师批改。', 0, '2026-05-28 09:49:00'),
(5, NULL, '成绩申诉已提交', '你的成绩复查申请已提交，教师会尽快处理。', 0, '2026-05-29 16:11:00'),
(NULL, 'STUDENT', '数据库强化练习发布', '数据库索引与事务强化试卷已发布，请相关班级同学按时完成。', 0, '2026-05-29 08:30:00');

INSERT INTO operation_log (user_id, username, role, action, target, detail, created_at) VALUES
(1, 'admin', 'ADMIN', '保存角色权限', 'TEACHER', '授予教师人工阅卷、成绩分析、考试监控权限', '2026-05-28 08:30:00'),
(2, 'teacher', 'TEACHER', '保存试卷', 'paper:Java综合主观题模拟', '发布包含简答题和编程题的综合练习', '2026-05-28 08:45:00'),
(3, 'student', 'STUDENT', '提交考试', 'paper:Java综合主观题模拟', '客观题自动得分，主观题进入待阅卷', '2026-05-28 09:48:00');

INSERT INTO backup_record (name, file_path, status, remark, created_at) VALUES
('开源演示数据备份', 'backup/campus_exam_ai_demo.sql', 'CREATED', '包含题库、试卷、成绩、监控、申诉与通知示例数据', '2026-05-29 18:00:00'),
('发布前数据快照', 'backup/campus_exam_ai_release.sql', 'RESTORED', '用于演示恢复流程的逻辑记录', '2026-05-30 10:00:00');

INSERT INTO announcement (title, content, creator_id, enabled) VALUES
('期末在线考试安排', '请同学们提前检查网络环境，考试开始后按时提交试卷。', 1, 1),
('AI错题解析上线', '学生提交试卷后可在错题解析中查看 AI 辅助讲解。', 1, 1),
('教师题库维护提醒', '请任课教师在考试前完成题库审核和试卷发布。', 1, 1);

INSERT INTO permission (code, name, module, path, sort_no) VALUES
('dashboard', '工作台', '工作台', '/', 1),
('admin:user', '用户管理', '系统管理', '/admin/users', 10),
('admin:class', '班级管理', '系统管理', '/admin/classes', 11),
('admin:course', '课程管理', '系统管理', '/admin/courses', 12),
('admin:teaching', '授课安排', '系统管理', '/admin/teacher-courses', 13),
('admin:announcement', '公告管理', '系统管理', '/admin/announcements', 14),
('admin:permission', '权限管理', '系统管理', '/admin/permissions', 15),
('teacher:questions', '题库管理', '教学管理', '/teacher/questions', 30),
('teacher:papers', '试卷管理', '教学管理', '/teacher/papers', 31),
('teacher:scores', '成绩管理', '教学管理', '/teacher/scores', 32),
('teacher:manual', '人工阅卷', '教学管理', '/teacher/manual', 33),
('teacher:analysis', '成绩分析', '教学管理', '/teacher/analysis', 34),
('teacher:monitor', '考试监控', '教学管理', '/teacher/monitor', 35),
('teacher:appeals', '成绩复查', '教学管理', '/teacher/appeals', 36),
('student:exams', '在线考试', '学生中心', '/student/exams', 50),
('student:attempts', '历史成绩', '学生中心', '/student/attempts', 51),
('student:wrong', '错题解析', '学生中心', '/student/wrong', 52),
('student:advice', '学习建议', '学生中心', '/student/advice', 53),
('student:appeals', '成绩申诉', '学生中心', '/student/appeals', 54),
('student:notifications', '通知中心', '学生中心', '/student/notifications', 55),
('student:profile', '个人中心', '学生中心', '/student/profile', 56),
('admin:logs', '操作日志', '系统管理', '/admin/logs', 80),
('admin:backup', '备份恢复', '系统管理', '/admin/backup', 81);

INSERT INTO role_permission (role, permission_code) VALUES
('ADMIN', 'dashboard'),
('ADMIN', 'admin:user'),
('ADMIN', 'admin:class'),
('ADMIN', 'admin:course'),
('ADMIN', 'admin:teaching'),
('ADMIN', 'admin:announcement'),
('ADMIN', 'admin:permission'),
('ADMIN', 'teacher:questions'),
('ADMIN', 'teacher:papers'),
('ADMIN', 'teacher:scores'),
('ADMIN', 'teacher:manual'),
('ADMIN', 'teacher:analysis'),
('ADMIN', 'teacher:monitor'),
('ADMIN', 'teacher:appeals'),
('ADMIN', 'student:exams'),
('ADMIN', 'student:attempts'),
('ADMIN', 'student:wrong'),
('ADMIN', 'student:advice'),
('ADMIN', 'student:appeals'),
('ADMIN', 'student:notifications'),
('ADMIN', 'student:profile'),
('ADMIN', 'admin:logs'),
('ADMIN', 'admin:backup'),
('TEACHER', 'dashboard'),
('TEACHER', 'teacher:questions'),
('TEACHER', 'teacher:papers'),
('TEACHER', 'teacher:scores'),
('TEACHER', 'teacher:manual'),
('TEACHER', 'teacher:analysis'),
('TEACHER', 'teacher:monitor'),
('TEACHER', 'teacher:appeals'),
('STUDENT', 'dashboard'),
('STUDENT', 'student:exams'),
('STUDENT', 'student:attempts'),
('STUDENT', 'student:wrong'),
('STUDENT', 'student:advice'),
('STUDENT', 'student:appeals'),
('STUDENT', 'student:notifications'),
('STUDENT', 'student:profile');
