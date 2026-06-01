import fs from 'node:fs/promises';
import path from 'node:path';
import { spawn } from 'node:child_process';

const ROOT = path.resolve(import.meta.dirname, '..');
const BASE = process.env.DEMO_BASE || 'http://localhost:5176';
const API = process.env.DEMO_API || 'http://localhost:18082/api';
const CHROME = process.env.CHROME_PATH || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const OUT_DIR = path.join(ROOT, 'demo-video', 'public', 'full-demo-screens');
const SCENES_FILE = path.join(ROOT, 'demo-video', 'public', 'full-demo-scenes.json');
const CAPTIONS_FILE = path.join(ROOT, 'demo-video', 'public', 'full-demo-captions.json');
const REPORT_FILE = path.join(ROOT, 'docs', 'full-demo-report.json');
const SCENE_MS = 3000;
const COVER_MS = 3000;
const MODULE_MS = 4000;
const ARCH_MS = 4000;
const INTRO_MS = COVER_MS + MODULE_MS + ARCH_MS;
const OUTRO_MS = 4000;

const report = {
  startedAt: new Date().toISOString(),
  api: {},
  created: {},
  screenshots: [],
};

await fs.mkdir(OUT_DIR, { recursive: true });

async function api(pathname, { token, method = 'GET', body, headers = {} } = {}) {
  const response = await fetch(`${API}${pathname}`, {
    method,
    headers: {
      ...(body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { 'X-Token': token } : {}),
      ...headers,
    },
    body: body instanceof FormData ? body : body == null ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  const data = text ? JSON.parse(text) : {};
  if (!data.success) {
    throw new Error(`${method} ${pathname}: ${data.message || response.status}`);
  }
  return data.data;
}

async function login(username, password = '123456') {
  const data = await api('/auth/login', { method: 'POST', body: { username, password } });
  report.api[`login:${username}`] = true;
  return data;
}

function formatDate(offsetMinutes = 0) {
  const d = new Date(Date.now() + offsetMinutes * 60_000);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

async function seedDemoData() {
  const stamp = Date.now();
  const admin = await login('admin');
  const teacher = await login('teacher');
  const student = await login('student2');

  const klass = await api('/admin/classes', {
    token: admin.token,
    method: 'POST',
    body: { name: '演示班级', grade: '2026', major: '智能考试演示' },
  });
  const updatedClass = await api('/admin/classes', {
    token: admin.token,
    method: 'POST',
    body: { ...klass, name: `${klass.name}-已修改` },
  });

  const course = await api('/admin/courses', {
    token: admin.token,
    method: 'POST',
    body: { code: `DEMO${String(stamp).slice(-5)}`, name: '演示课程', description: '用于完整功能演示的课程。' },
  });
  const updatedCourse = await api('/admin/courses', {
    token: admin.token,
    method: 'POST',
    body: { ...course, description: '用于完整功能演示的课程，已完成编辑保存。' },
  });

  const demoUser = await api('/admin/users', {
    token: admin.token,
    method: 'POST',
    body: {
      username: `demo_student_${stamp}`,
      password: '123456',
      realName: '演示学生',
      role: 'STUDENT',
      classId: 1,
      phone: '13900000000',
      email: `demo_${stamp}@example.com`,
      enabled: 1,
    },
  });
  const updatedUser = await api('/admin/users', {
    token: admin.token,
    method: 'POST',
    body: { ...demoUser, password: '', realName: '演示学生-已修改' },
  });

  const teacherCourse = await api('/admin/teacher-courses', {
    token: admin.token,
    method: 'POST',
    body: { teacherId: 2, courseId: updatedCourse.id, classId: updatedClass.id },
  });
  const announcement = await api('/admin/announcements', {
    token: admin.token,
    method: 'POST',
    body: { title: '完整演示公告', content: '这是一条用于视频演示的公告。', enabled: 1 },
  });
  await api('/admin/announcements', {
    token: admin.token,
    method: 'POST',
    body: { ...announcement, content: '这是一条用于视频演示的公告，已完成编辑。' },
  });
  const teacherPermissions = await api('/admin/role-permissions/TEACHER', { token: admin.token });
  await api('/admin/role-permissions/TEACHER', { token: admin.token, method: 'POST', body: teacherPermissions });
  const backup = await api('/admin/backup-records', {
    token: admin.token,
    method: 'POST',
    body: { remark: '完整演示自动创建备份记录' },
  });
  await api(`/admin/backup-records/${backup.id}/restore`, { token: admin.token, method: 'POST' });
  await api('/admin/notifications', {
    token: admin.token,
    method: 'POST',
    body: { role: 'STUDENT', title: '完整演示通知', content: '请按时参加在线考试并查看成绩。' },
  });

  const singleQuestion = await api('/teacher/questions', {
    token: teacher.token,
    method: 'POST',
    body: {
      question: {
        courseId: 1,
        type: 'SINGLE',
        stem: '完整演示新增单选题',
        correctAnswer: 'A',
        analysis: 'A 是正确答案，演示自动评分和错题解析。',
        difficulty: 2,
        knowledgePoint: '完整演示知识点',
        reviewStatus: 'DRAFT',
      },
      options: [
        { optionKey: 'A', optionText: '正确选项' },
        { optionKey: 'B', optionText: '干扰选项' },
        { optionKey: 'C', optionText: '干扰选项' },
        { optionKey: 'D', optionText: '干扰选项' },
      ],
    },
  });
  const updatedSingleQuestion = await api('/teacher/questions', {
    token: teacher.token,
    method: 'POST',
    body: {
      ...singleQuestion,
      question: {
        ...singleQuestion.question,
        stem: '完整演示编辑后的单选题',
        reviewStatus: 'APPROVED',
      },
    },
  });
  const shortQuestion = await api('/teacher/questions', {
    token: teacher.token,
    method: 'POST',
    body: {
      question: {
        courseId: 1,
        type: 'SHORT',
        stem: '完整演示主观题',
        correctAnswer: '围绕概念、流程和风险说明。',
        analysis: '用于演示主观题提交、人工阅卷和批注。',
        difficulty: 3,
        knowledgePoint: '主观题阅卷',
        reviewStatus: 'APPROVED',
      },
      options: [],
    },
  });
  const aiQuestions = await api('/teacher/ai/questions', {
    token: teacher.token,
    method: 'POST',
    body: { courseId: 1, topic: 'Java 集合完整演示', type: 'SINGLE', count: 2, difficulty: 2 },
  });
  const aiPaper = await api('/teacher/ai/paper', {
    token: teacher.token,
    method: 'POST',
    body: {
      courseId: 1,
      title: 'AI完整演示组卷',
      topic: '面向对象基础和集合框架',
      questionCount: 4,
      totalScore: 40,
      durationMinutes: 30,
      singleCount: 1,
      multipleCount: 1,
      judgeCount: 1,
      shortCount: 1,
      programCount: 0,
      easyCount: 1,
      mediumCount: 2,
      hardCount: 1,
      knowledgePoints: ['集合框架', '面向对象基础'],
    },
  });
  const paper = await api('/teacher/papers', {
    token: teacher.token,
    method: 'POST',
    body: {
      paper: {
        courseId: 1,
        title: '完整演示试卷',
        durationMinutes: 30,
        published: 1,
        allowRetake: 1,
        startTime: formatDate(-15),
        endTime: formatDate(24 * 60),
      },
      questions: [
        { questionId: updatedSingleQuestion.question.id, score: 10, sortNo: 1 },
        { questionId: shortQuestion.question.id, score: 10, sortNo: 2 },
      ],
    },
  });
  const updatedPaper = await api('/teacher/papers', {
    token: teacher.token,
    method: 'POST',
    body: {
      paper: { ...paper.paper, title: `${paper.paper.title}-已编辑`, published: 1, allowRetake: 1 },
      questions: paper.questions,
    },
  });

  const started = await api(`/student/exams/${updatedPaper.paper.id}/start`, { token: student.token, method: 'POST' });
  await api('/student/monitor', {
    token: student.token,
    method: 'POST',
    body: { attemptId: started.attempt.id, eventType: 'FULLSCREEN_EXIT', detail: '完整演示：学生退出全屏。' },
  });
  await api('/student/monitor', {
    token: student.token,
    method: 'POST',
    body: { attemptId: started.attempt.id, eventType: 'BLUR', detail: '完整演示：窗口失焦。' },
  });
  const submitted = await api(`/student/attempts/${started.attempt.id}/submit`, {
    token: student.token,
    method: 'POST',
    body: {
      answers: [
        { questionId: updatedSingleQuestion.question.id, answer: 'B' },
        { questionId: shortQuestion.question.id, answer: '这是完整演示提交的主观题答案。' },
      ],
    },
  });
  const manualRows = await api('/teacher/manual', { token: teacher.token });
  const manualTarget = manualRows.find((row) => row.attemptId === started.attempt.id && row.question.id === shortQuestion.question.id);
  if (manualTarget) {
    await api(`/teacher/manual/${manualTarget.answerId}`, {
      token: teacher.token,
      method: 'POST',
      body: { score: 8, comment: '完整演示批注：表达清楚，补充细节更好。' },
    });
  }
  const appeal = await api('/student/appeals', {
    token: student.token,
    method: 'POST',
    body: { attemptId: started.attempt.id, reason: '完整演示：申请复核主观题得分。' },
  });
  await api(`/teacher/appeals/${appeal.id}`, {
    token: teacher.token,
    method: 'POST',
    body: { status: 'APPROVED', reply: '完整演示：已复核并通过。' },
  });
  const profile = await api('/student/profile', { token: student.token });
  await api('/student/profile', {
    token: student.token,
    method: 'POST',
    body: { ...profile, realName: '赵同学-演示资料', phone: '13899990000', email: 'student-demo@example.com' },
  });
  const notifications = await api('/student/notifications', { token: student.token });
  const unread = notifications.find((item) => !item.readFlag);
  if (unread) {
    await api(`/student/notifications/${unread.id}/read`, { token: student.token, method: 'POST' });
  }

  Object.assign(report.created, {
    classId: updatedClass.id,
    courseId: updatedCourse.id,
    userId: updatedUser.id,
    teacherCourseId: teacherCourse.id,
    backupId: backup.id,
    singleQuestionId: updatedSingleQuestion.question.id,
    shortQuestionId: shortQuestion.question.id,
    aiQuestionCount: aiQuestions.length,
    aiPaperQuestionCount: aiPaper.questions.length,
    paperId: updatedPaper.paper.id,
    paperTitle: updatedPaper.paper.title,
    attemptId: submitted.id,
    appealId: appeal.id,
  });
}

class CdpClient {
  constructor(ws) {
    this.ws = ws;
    this.id = 0;
    this.pending = new Map();
    this.events = new Map();
    ws.addEventListener('message', (message) => {
      const data = JSON.parse(message.data);
      if (data.id && this.pending.has(data.id)) {
        const { resolve, reject } = this.pending.get(data.id);
        this.pending.delete(data.id);
        data.error ? reject(new Error(data.error.message)) : resolve(data.result);
      } else if (data.method) {
        const listeners = this.events.get(data.method) || [];
        listeners.splice(0).forEach((resolve) => resolve(data.params || {}));
      }
    });
  }

  send(method, params = {}) {
    const id = ++this.id;
    this.ws.send(JSON.stringify({ id, method, params }));
    return new Promise((resolve, reject) => this.pending.set(id, { resolve, reject }));
  }

  once(event) {
    return new Promise((resolve) => {
      if (!this.events.has(event)) this.events.set(event, []);
      this.events.get(event).push(resolve);
    });
  }
}

async function waitForDebug(port) {
  for (let i = 0; i < 80; i++) {
    try {
      const response = await fetch(`http://127.0.0.1:${port}/json/version`);
      if (response.ok) return response.json();
    } catch {}
    await new Promise((resolve) => setTimeout(resolve, 150));
  }
  throw new Error('Chrome remote debugging did not start');
}

async function runScreenshots() {
  const scenes = [];
  const port = 9550 + Math.floor(Math.random() * 200);
  const userDataDir = path.join('/tmp', `campus-exam-full-demo-${Date.now()}`);
  const chrome = spawn(CHROME, [
    '--headless=new',
    `--remote-debugging-port=${port}`,
    `--user-data-dir=${userDataDir}`,
    '--disable-gpu',
    '--no-first-run',
    '--window-size=1440,810',
    'about:blank',
  ], { stdio: 'ignore' });

  try {
    await waitForDebug(port);
    const targets = await fetch(`http://127.0.0.1:${port}/json`).then((r) => r.json());
    const target = targets.find((item) => item.type === 'page') || targets[0];
    const ws = new WebSocket(target.webSocketDebuggerUrl);
    await new Promise((resolve, reject) => {
      ws.addEventListener('open', resolve, { once: true });
      ws.addEventListener('error', reject, { once: true });
    });
    const cdp = new CdpClient(ws);
    await cdp.send('Page.enable');
    await cdp.send('Runtime.enable');
    await cdp.send('Emulation.setDeviceMetricsOverride', {
      width: 1440,
      height: 810,
      deviceScaleFactor: 1,
      mobile: false,
    });

    async function sleep(ms) {
      await new Promise((resolve) => setTimeout(resolve, ms));
    }

    async function evaluate(expression) {
      return cdp.send('Runtime.evaluate', {
        awaitPromise: true,
        returnByValue: true,
        expression,
      });
    }

    async function navigate(url) {
      const loaded = cdp.once('Page.loadEventFired');
      await cdp.send('Page.navigate', { url });
      await loaded;
      await sleep(1000);
      await evaluate('window.scrollTo(0, 0)');
    }

    async function pageLogin(username) {
      await navigate(`${BASE}/login`);
      await evaluate(`
        (async () => {
          const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: '${username}', password: '123456' })
          }).then(r => r.json());
          localStorage.setItem('token', res.data.token);
          localStorage.setItem('user', JSON.stringify(res.data.user));
          localStorage.setItem('permissions', JSON.stringify(res.data.permissions || []));
        })()
      `);
    }

    async function clickText(text, index = 0) {
      const escaped = JSON.stringify(text);
      const result = await evaluate(`
        (() => {
          const nodes = [...document.querySelectorAll('button, .el-button, [role="button"], a, label')];
          const matches = nodes.filter((node) => {
            const rect = node.getBoundingClientRect();
            return rect.width > 0 && rect.height > 0 && (node.innerText || node.textContent || '').includes(${escaped});
          });
          const target = matches[${index}] || matches[0];
          if (!target) return false;
          target.scrollIntoView({ block: 'center', inline: 'center' });
          target.click();
          return true;
        })()
      `);
      await sleep(700);
      return result.result?.value;
    }

    async function clickExamByTitle(title) {
      const result = await evaluate(`
        (() => {
          const title = ${JSON.stringify(title)};
          const cards = [...document.querySelectorAll('.exam-card')];
          const card = cards.find((node) => (node.innerText || '').includes(title));
          const button = card ? [...card.querySelectorAll('button, .el-button')].find((node) => (node.innerText || '').includes('开始考试')) : null;
          if (!button) return false;
          button.scrollIntoView({ block: 'center', inline: 'center' });
          button.click();
          return true;
        })()
      `);
      await sleep(700);
      return result.result?.value;
    }

    async function fillFirstTextarea(value) {
      await evaluate(`
        (() => {
          const input = document.querySelector('textarea');
          if (!input) return false;
          input.value = ${JSON.stringify(value)};
          input.dispatchEvent(new Event('input', { bubbles: true }));
          return true;
        })()
      `);
      await sleep(300);
    }

    async function shot(name, scene) {
      await sleep(250);
      const png = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
      const filename = `${String(scenes.length + 1).padStart(2, '0')}-${name}.png`;
      await fs.writeFile(path.join(OUT_DIR, filename), Buffer.from(png.data, 'base64'));
      const fullScene = { image: filename, ...scene };
      scenes.push(fullScene);
      report.screenshots.push(fullScene);
    }

    await navigate(`${BASE}/login`);
    await shot('login', {
      title: '登录页与演示账号',
      role: '全角色',
      tags: ['管理员', '教师', '学生'],
      caption: '系统提供管理员、教师、学生三类入口，方便演示完整业务闭环。',
    });

    await pageLogin('admin');
    await navigate(`${BASE}/`);
    await shot('admin-dashboard', {
      title: '管理员工作台',
      role: '管理员',
      tags: ['用户规模', '题库结构', '考试概览'],
      caption: '管理员工作台汇总用户、课程、题库、试卷和考试运行数据。',
    });
    await navigate(`${BASE}/admin/users`);
    await shot('admin-users-list', {
      title: '用户管理列表',
      role: '管理员',
      tags: ['查询', '导出', '启用状态'],
      caption: '用户管理支持查看账号、角色、联系方式和启用状态。',
    });
    await clickText('新增用户');
    await shot('admin-user-add', {
      title: '新增用户',
      role: '管理员',
      tags: ['新增', '角色', '初始密码'],
      caption: '新增用户时可以设置角色、班级、联系方式和初始密码。',
    });
    await navigate(`${BASE}/admin/users`);
    await clickText('编辑');
    await shot('admin-user-edit', {
      title: '编辑用户',
      role: '管理员',
      tags: ['修改', '停用启用', '资料维护'],
      caption: '编辑用户可修改姓名、电话、邮箱、角色和启用状态。',
    });

    await navigate(`${BASE}/admin/classes`);
    await shot('admin-classes-list', {
      title: '班级管理',
      role: '管理员',
      tags: ['班级', '年级', '专业'],
      caption: '班级管理维护学生所属班级、年级和专业信息。',
    });
    await clickText('新增班级');
    await shot('admin-class-add', {
      title: '新增班级',
      role: '管理员',
      tags: ['新增', '年级', '专业'],
      caption: '管理员可以新增班级，为学生和授课安排提供基础数据。',
    });
    await navigate(`${BASE}/admin/courses`);
    await shot('admin-courses-list', {
      title: '课程管理',
      role: '管理员',
      tags: ['课程编号', '课程简介', '导出'],
      caption: '课程管理维护课程编号、课程名称和课程简介。',
    });
    await clickText('新增课程');
    await shot('admin-course-add', {
      title: '新增课程',
      role: '管理员',
      tags: ['新增', '编号', '简介'],
      caption: '新增课程后，教师题库和试卷可以按课程归类。',
    });
    await navigate(`${BASE}/admin/courses`);
    await clickText('编辑');
    await shot('admin-course-edit', {
      title: '编辑课程',
      role: '管理员',
      tags: ['修改', '课程信息', '保存'],
      caption: '课程信息支持编辑保存，操作会进入系统日志。',
    });
    await navigate(`${BASE}/admin/teacher-courses`);
    await shot('admin-teaching-list', {
      title: '授课安排',
      role: '管理员',
      tags: ['教师', '课程', '班级绑定'],
      caption: '授课安排把教师、课程和班级绑定起来，决定学生可参加的考试范围。',
    });
    await clickText('新增安排');
    await shot('admin-teaching-add', {
      title: '新增授课安排',
      role: '管理员',
      tags: ['新增', '教师课程', '班级覆盖'],
      caption: '新增授课安排后，相关班级学生可以看到对应课程试卷。',
    });
    await navigate(`${BASE}/admin/announcements`);
    await shot('admin-announcements-list', {
      title: '公告管理',
      role: '管理员',
      tags: ['公告', '显示隐藏', '工作台'],
      caption: '公告会展示在工作台，用于考试安排和系统通知。',
    });
    await clickText('发布公告');
    await shot('admin-announcement-add', {
      title: '发布公告',
      role: '管理员',
      tags: ['新增', '标题', '内容'],
      caption: '管理员可以发布公告，控制公告是否显示。',
    });
    await navigate(`${BASE}/admin/permissions`);
    await shot('admin-permissions', {
      title: '权限管理',
      role: '管理员',
      tags: ['角色权限', '菜单控制', '保存'],
      caption: '权限管理按角色勾选功能菜单，用户重新登录后生效。',
    });
    await navigate(`${BASE}/admin/logs`);
    await shot('admin-logs', {
      title: '操作日志',
      role: '管理员',
      tags: ['审计', '动作', '目标'],
      caption: '关键操作会记录到日志，便于追踪管理员、教师和学生行为。',
    });
    await navigate(`${BASE}/admin/backup`);
    await shot('admin-backup', {
      title: '备份恢复',
      role: '管理员',
      tags: ['创建备份', '恢复', '记录'],
      caption: '备份恢复页面记录逻辑备份和恢复状态，适合演示数据管理。',
    });

    await pageLogin('teacher');
    await navigate(`${BASE}/`);
    await shot('teacher-dashboard', {
      title: '教师工作台',
      role: '教师',
      tags: ['待阅卷', '申诉', '班级覆盖'],
      caption: '教师工作台突出待人工阅卷、待处理申诉和授课班级覆盖。',
    });
    await navigate(`${BASE}/teacher/questions`);
    await shot('teacher-questions-list', {
      title: '题库管理',
      role: '教师',
      tags: ['多题型', '导入', '审核'],
      caption: '题库覆盖客观题、简答题和编程题，并支持审核状态维护。',
    });
    await clickText('新增题目');
    await shot('teacher-question-add', {
      title: '新增题目',
      role: '教师',
      tags: ['新增', '题型', '知识点'],
      caption: '新增题目时可以选择题型、难度、知识点和解析。',
    });
    await navigate(`${BASE}/teacher/questions`);
    await clickText('编辑');
    await shot('teacher-question-edit', {
      title: '编辑题目',
      role: '教师',
      tags: ['修改', '选项', '审核状态'],
      caption: '教师可编辑题干、选项、答案、解析和审核状态。',
    });
    await navigate(`${BASE}/teacher/questions`);
    await clickText('AI出题');
    await shot('teacher-ai-question', {
      title: 'AI 出题',
      role: '教师',
      tags: ['讯飞星火', '题型', '难度'],
      caption: 'AI 出题按课程、知识点、题型、数量和难度生成题目草稿。',
    });
    await navigate(`${BASE}/teacher/questions`);
    await clickText('查重');
    await shot('teacher-question-duplicate', {
      title: '题目查重',
      role: '教师',
      tags: ['查重', '重复题干', '质量控制'],
      caption: '题库查重用于发现重复题干，提升题库维护质量。',
    });
    await navigate(`${BASE}/teacher/papers`);
    await shot('teacher-papers-list', {
      title: '试卷管理',
      role: '教师',
      tags: ['考试时间', '重考', '发布'],
      caption: '试卷列表展示时长、总分、发布状态和是否允许重考。',
    });
    await clickText('新增试卷');
    await shot('teacher-paper-add', {
      title: '新增试卷',
      role: '教师',
      tags: ['组卷', '分值', '考试时间'],
      caption: '手动组卷可勾选题目、设置分值、配置考试时间和重考规则。',
    });
    await navigate(`${BASE}/teacher/papers`);
    await clickText('编辑');
    await shot('teacher-paper-edit', {
      title: '编辑试卷',
      role: '教师',
      tags: ['修改', '发布开关', '题目分值'],
      caption: '教师可以编辑试卷名称、时长、发布状态和题目分值。',
    });
    await navigate(`${BASE}/teacher/papers`);
    await clickText('AI出卷');
    await shot('teacher-ai-paper', {
      title: 'AI 智能组卷',
      role: '教师',
      tags: ['题型比例', '难度比例', '知识点覆盖'],
      caption: 'AI 组卷支持题型比例、难度比例和知识点覆盖约束。',
    });
    await navigate(`${BASE}/teacher/scores`);
    await shot('teacher-scores', {
      title: '成绩管理',
      role: '教师',
      tags: ['客观题', '主观题', '提交状态'],
      caption: '成绩管理汇总学生提交、客观题得分、主观题得分和复核状态。',
    });
    await navigate(`${BASE}/teacher/manual`);
    await shot('teacher-manual', {
      title: '人工阅卷',
      role: '教师',
      tags: ['简答题', '编程题', '批注'],
      caption: '人工阅卷可录入主观题分数和教师批注，并重新计算总分。',
    });
    await navigate(`${BASE}/teacher/analysis`);
    await shot('teacher-analysis', {
      title: '成绩分析报表',
      role: '教师',
      tags: ['均分', '正确率', '薄弱知识点'],
      caption: '成绩分析展示班级统计、题目正确率、及格率和薄弱知识点。',
    });
    await navigate(`${BASE}/teacher/monitor`);
    await shot('teacher-monitor', {
      title: '考试监控',
      role: '教师',
      tags: ['切屏', '全屏退出', 'IP 设备'],
      caption: '监控页面记录切屏、失焦、全屏异常、IP 和设备信息。',
    });
    await navigate(`${BASE}/teacher/appeals`);
    await shot('teacher-appeals', {
      title: '成绩复查处理',
      role: '教师',
      tags: ['申诉', '通过驳回', '复核意见'],
      caption: '教师可以处理学生成绩复查申请，并记录处理结果。',
    });

    await pageLogin('student2');
    await navigate(`${BASE}/`);
    await shot('student-dashboard', {
      title: '学生工作台',
      role: '学生',
      tags: ['可参加考试', '待复盘错题', '薄弱知识点'],
      caption: '学生工作台展示可参加考试、历史表现、待复盘错题和薄弱知识点。',
    });
    await navigate(`${BASE}/student/exams`);
    await shot('student-exams-list', {
      title: '在线考试列表',
      role: '学生',
      tags: ['可考试', '开始考试', '重考规则'],
      caption: '在线考试列表按班级和课程展示当前可参加试卷。',
    });
    if (!(await clickExamByTitle(report.created.paperTitle))) {
      await clickText('开始考试');
    }
    await clickText('确定');
    await sleep(1200);
    await evaluate('document.querySelector(".exam")?.scrollIntoView({ block: "start" })');
    await shot('student-exam-running', {
      title: '考试作答页',
      role: '学生',
      tags: ['倒计时', '答题进度', '全屏'],
      caption: '进入考试后展示倒计时、答题进度、全屏按钮和题目作答区。',
    });
    await fillFirstTextarea('这是视频演示中在考试界面填写的主观题答案。');
    await evaluate('document.querySelector(".exam")?.scrollIntoView({ block: "start" })');
    await shot('student-exam-answering', {
      title: '学生作答',
      role: '学生',
      tags: ['主观题', '答案输入', '提交试卷'],
      caption: '学生可以完成客观题选择和主观题输入，提交后进入评分流程。',
    });
    await navigate(`${BASE}/student/attempts`);
    await shot('student-attempts', {
      title: '历史成绩',
      role: '学生',
      tags: ['成绩', '状态', '提交时间'],
      caption: '历史成绩保存每次考试记录，方便学生回看得分和提交状态。',
    });
    await navigate(`${BASE}/student/wrong`);
    await shot('student-wrong', {
      title: '错题解析',
      role: '学生',
      tags: ['错题', '答案', '解析'],
      caption: '错题解析展示学生答案、正确答案和原始解析。',
    });
    await clickText('AI解析');
    await sleep(1400);
    await shot('student-wrong-ai', {
      title: 'AI 错题解析',
      role: '学生',
      tags: ['AI 解析', '错因', '复习建议'],
      caption: '学生可以生成 AI 错题解析，理解错因并获得复习建议。',
    });
    await navigate(`${BASE}/student/advice`);
    await shot('student-advice', {
      title: '个性化学习建议',
      role: '学生',
      tags: ['知识点清单', '错题统计', '练习方向'],
      caption: '学习建议根据错题知识点自动生成复习清单和练习方向。',
    });
    await navigate(`${BASE}/student/appeals`);
    await shot('student-appeals', {
      title: '成绩申诉',
      role: '学生',
      tags: ['选择考试', '填写原因', '处理记录'],
      caption: '学生可以对成绩提出复查申请，并查看教师处理回复。',
    });
    await navigate(`${BASE}/student/notifications`);
    await shot('student-notifications', {
      title: '通知中心',
      role: '学生',
      tags: ['考试提醒', '成绩通知', '已读未读'],
      caption: '通知中心集中展示考试、复核和系统消息，并支持标记已读。',
    });
    await navigate(`${BASE}/student/profile`);
    await shot('student-profile', {
      title: '个人中心',
      role: '学生',
      tags: ['资料修改', '密码修改', '保存'],
      caption: '个人中心支持修改基本资料和更新密码。',
    });

    ws.close();
    return scenes;
  } finally {
    chrome.kill('SIGTERM');
  }
}

function buildCaptions(scenes) {
  const captions = [
    {
      text: '校园智能在线考试系统完整功能演示，覆盖新增、修改、考试、阅卷、分析和通知全流程。',
      startMs: 0,
      endMs: COVER_MS,
      timestampMs: null,
      confidence: null,
    },
    {
      text: '功能模块图先展示管理员、教师、学生三端能力，方便快速理解系统边界。',
      startMs: COVER_MS,
      endMs: COVER_MS + MODULE_MS,
      timestampMs: null,
      confidence: null,
    },
    {
      text: '系统框架图展示 Vue 前端、Spring Boot 后端、MySQL 数据库和讯飞星火 AI 服务的协作关系。',
      startMs: COVER_MS + MODULE_MS,
      endMs: INTRO_MS,
      timestampMs: null,
      confidence: null,
    },
  ];
  scenes.forEach((scene, index) => {
    const startMs = INTRO_MS + index * SCENE_MS;
    captions.push({
      text: scene.caption,
      startMs,
      endMs: startMs + SCENE_MS,
      timestampMs: null,
      confidence: null,
    });
  });
  const outroStart = INTRO_MS + scenes.length * SCENE_MS;
  captions.push({
    text: '演示完成：项目已包含完整 SQL、教程截图、自动化测试报告和 Remotion 视频工程。',
    startMs: outroStart,
    endMs: outroStart + OUTRO_MS,
    timestampMs: null,
    confidence: null,
  });
  return captions;
}

try {
  await seedDemoData();
  const scenes = await runScreenshots();
  const captions = buildCaptions(scenes);
  report.finishedAt = new Date().toISOString();
  await fs.writeFile(SCENES_FILE, JSON.stringify(scenes, null, 2));
  await fs.writeFile(CAPTIONS_FILE, JSON.stringify(captions, null, 2));
  await fs.writeFile(REPORT_FILE, JSON.stringify(report, null, 2));
  console.log(JSON.stringify({
    scenes: scenes.length,
    captions: captions.length,
    report: path.relative(ROOT, REPORT_FILE),
    scenesFile: path.relative(ROOT, SCENES_FILE),
    captionsFile: path.relative(ROOT, CAPTIONS_FILE),
  }, null, 2));
} catch (error) {
  report.error = error.stack || String(error);
  await fs.writeFile(REPORT_FILE, JSON.stringify(report, null, 2));
  console.error(report.error);
  process.exit(1);
}
