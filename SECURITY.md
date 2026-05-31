# 安全说明

## 支持范围

当前项目主要用于学习、毕业设计和二次开发演示。开源版本不承诺生产级安全保障。

## 敏感信息

- 不要提交真实的 `application-local.yml`。
- 推荐使用环境变量配置数据库和 AI Key：

```bash
export DB_URL="jdbc:mysql://localhost:3306/campus_exam_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
export DB_USERNAME="root"
export DB_PASSWORD="your_password"
export SPARK_API_PASSWORD="your_spark_api_password"
```

## 已有安全措施

- 登录 token 有过期时间。
- 新增或修改密码时使用 BCrypt 加密。
- 后端接口按角色进行访问控制。
- 前端根据权限隐藏不可访问菜单。
- 操作日志记录关键管理、组卷、考试、阅卷和申诉动作。

## 生产部署建议

- 使用 HTTPS。
- 将 token 存储迁移到更安全的会话方案。
- 为登录增加验证码、失败锁定和审计告警。
- 为考试监控、题库、成绩数据增加更细粒度权限。
- 定期备份数据库，生产环境请接入物理备份方案。
