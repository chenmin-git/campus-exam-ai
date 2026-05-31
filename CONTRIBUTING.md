# 贡献指南

感谢你关注校园智能在线考试系统。这个项目适合作为毕业设计、课程设计和在线考试系统二次开发基础。

## 本地开发

1. 初始化数据库：

```bash
mysql -uroot -proot < sql/campus_exam_ai.sql
```

2. 配置环境变量或复制示例配置：

```bash
cp backend/src/main/resources/application-example.yml backend/src/main/resources/application-local.yml
```

3. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

4. 启动前端：

```bash
cd frontend
npm install
npm run dev
```

## 提交建议

- 提交前请运行后端构建：`cd backend && mvn -q -DskipTests package`。
- 提交前请运行前端构建：`cd frontend && npm run build`。
- 不要提交 `frontend/node_modules`、`frontend/dist`、`backend/target`、日志文件和本地密钥。
- 新功能建议同步更新 `README.md`、`docs/功能清单.md` 和 `教程.html`。

## 代码风格

- 后端遵循现有 Spring Boot + MyBatis-Plus 分层：Controller 只做接口编排，核心规则放在 Service。
- 前端遵循现有 Vue 3 组合式 API 写法，页面放在 `frontend/src/views`。
- 数据库字段使用下划线命名，Java 实体使用驼峰命名。

## 安全提醒

请不要在 issue、PR 或提交记录中包含真实数据库密码、AI API Key、服务器 IP、学生隐私数据或考试真实题库。
