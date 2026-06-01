import fs from 'node:fs/promises';
import path from 'node:path';
import { spawn } from 'node:child_process';

const ROOT = path.resolve(import.meta.dirname, '..');
const BASE = 'http://localhost:5176';
const API = 'http://localhost:18082/api';
const OUT_DIR = path.join(ROOT, 'docs', 'tutorial-assets');
const CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const REPORT = {
  startedAt: new Date().toISOString(),
  api: {},
  screenshots: [],
  created: {}
};

await fs.mkdir(OUT_DIR, { recursive: true });

async function api(pathname, { token, method = 'GET', body, headers = {} } = {}) {
  const response = await fetch(`${API}${pathname}`, {
    method,
    headers: {
      ...(body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { 'X-Token': token } : {}),
      ...headers
    },
    body: body instanceof FormData ? body : body == null ? undefined : JSON.stringify(body)
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
  REPORT.api[`login:${username}`] = true;
  return data;
}

function formatDate(offsetMinutes = 0) {
  const d = new Date(Date.now() + offsetMinutes * 60_000);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

async function runApiSmoke() {
  const admin = await login('admin');
  const teacher = await login('teacher');
  const student = await login('student2');

  REPORT.api.adminUsers = (await api('/admin/users', { token: admin.token })).length;
  REPORT.api.adminPermissions = (await api('/admin/permissions', { token: admin.token })).length;
  const backup = await api('/admin/backup-records', {
    token: admin.token,
    method: 'POST',
    body: { remark: '教程自动化测试备份' }
  });
  await api(`/admin/backup-records/${backup.id}/restore`, { token: admin.token, method: 'POST' });
  await api('/admin/notifications', {
    token: admin.token,
    method: 'POST',
    body: { role: 'STUDENT', title: '教程测试通知', content: '这是一条用于教程截图的通知。' }
  });
  REPORT.created.backupId = backup.id;

  const form = new FormData();
  const xlsx = await fs.readFile(path.join(OUT_DIR, 'import-template.xlsx'));
  form.append('file', new Blob([xlsx], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), 'import-template.xlsx');
  const importResult = await api('/teacher/questions/import', { token: teacher.token, method: 'POST', body: form });
  REPORT.api.questionImport = importResult;

  const questionDraft = {
    question: {
      courseId: 1,
      type: 'SHORT',
      stem: '教程测试主观题',
      correctAnswer: '',
      analysis: '从核心概念、使用场景和注意事项三个角度作答。',
      difficulty: 2,
      knowledgePoint: '教程测试知识点',
      reviewStatus: 'APPROVED'
    },
    options: []
  };
  const savedQuestion = await api('/teacher/questions', { token: teacher.token, method: 'POST', body: questionDraft });
  REPORT.created.questionId = savedQuestion.question.id;

  const paperPayload = {
    paper: {
      courseId: 1,
      title: '教程测试主观题试卷',
      durationMinutes: 30,
      published: 1,
      allowRetake: 1,
      startTime: formatDate(-10),
      endTime: formatDate(24 * 60)
    },
    questions: [{ questionId: savedQuestion.question.id, score: 10, sortNo: 1 }]
  };
  const savedPaper = await api('/teacher/papers', { token: teacher.token, method: 'POST', body: paperPayload });
  REPORT.created.paperId = savedPaper.paper.id;

  const started = await api(`/student/exams/${savedPaper.paper.id}/start`, { token: student.token, method: 'POST' });
  REPORT.created.attemptId = started.attempt.id;
  await api('/student/monitor', {
    token: student.token,
    method: 'POST',
    body: { attemptId: started.attempt.id, eventType: 'VISIBILITY_CHANGE', detail: '教程自动化测试切屏记录' }
  });
  await api(`/student/attempts/${started.attempt.id}/submit`, {
    token: student.token,
    method: 'POST',
    body: { answers: [{ questionId: savedQuestion.question.id, answer: '这是教程自动化测试提交的主观题答案。' }] }
  });

  const manualRows = await api('/teacher/manual', { token: teacher.token });
  const targetManual = manualRows.find((row) => row.attemptId === started.attempt.id);
  if (!targetManual) throw new Error('人工阅卷测试数据未出现');
  await api(`/teacher/manual/${targetManual.answerId}`, {
    token: teacher.token,
    method: 'POST',
    body: { score: 8, comment: '教程自动化测试批注：观点清楚。' }
  });

  const appeal = await api('/student/appeals', {
    token: student.token,
    method: 'POST',
    body: { attemptId: started.attempt.id, reason: '教程自动化测试申诉：申请复核主观题。' }
  });
  await api(`/teacher/appeals/${appeal.id}`, {
    token: teacher.token,
    method: 'POST',
    body: { status: 'APPROVED', reply: '教程自动化测试：已复核。' }
  });
  REPORT.created.appealId = appeal.id;

  REPORT.api.teacherAnalysis = (await api('/teacher/analysis', { token: teacher.token })).length;
  REPORT.api.teacherMonitor = (await api('/teacher/monitor', { token: teacher.token })).length;
  REPORT.api.teacherAppeals = (await api('/teacher/appeals', { token: teacher.token })).length;
  REPORT.api.studentAdvice = (await api('/student/study-advice', { token: student.token })).length;
  REPORT.api.studentNotifications = (await api('/student/notifications', { token: student.token })).length;
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
  const port = 9333 + Math.floor(Math.random() * 200);
  const userDataDir = path.join('/tmp', `campus-exam-chrome-${Date.now()}`);
  const chrome = spawn(CHROME, [
    '--headless=new',
    `--remote-debugging-port=${port}`,
    `--user-data-dir=${userDataDir}`,
    '--disable-gpu',
    '--no-first-run',
    '--window-size=1440,1000',
    'about:blank'
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
    await cdp.send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });

    async function navigate(url) {
      const loaded = cdp.once('Page.loadEventFired');
      await cdp.send('Page.navigate', { url });
      await loaded;
      await new Promise((resolve) => setTimeout(resolve, 900));
    }

    async function pageLogin(username) {
      await navigate(`${BASE}/login`);
      await cdp.send('Runtime.evaluate', {
        awaitPromise: true,
        expression: `
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
        `
      });
    }

    async function shot(name, url) {
      await navigate(url);
      const png = await cdp.send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true });
      const rel = `tutorial-assets/${name}.png`;
      await fs.writeFile(path.join(OUT_DIR, `${name}.png`), Buffer.from(png.data, 'base64'));
      REPORT.screenshots.push({ name, path: rel, url });
    }

    await shot('01-login', `${BASE}/login`);
    await pageLogin('admin');
    await shot('02-admin-users', `${BASE}/admin/users`);
    await shot('03-admin-permissions', `${BASE}/admin/permissions`);
    await shot('04-admin-logs-backup', `${BASE}/admin/backup`);

    await pageLogin('teacher');
    await shot('05-teacher-questions', `${BASE}/teacher/questions`);
    await shot('06-teacher-papers', `${BASE}/teacher/papers`);
    await shot('07-teacher-scores', `${BASE}/teacher/scores`);
    await shot('08-teacher-manual', `${BASE}/teacher/manual`);
    await shot('09-teacher-analysis', `${BASE}/teacher/analysis`);
    await shot('10-teacher-monitor', `${BASE}/teacher/monitor`);
    await shot('11-teacher-appeals', `${BASE}/teacher/appeals`);

    await pageLogin('student');
    await shot('12-student-exams', `${BASE}/student/exams`);
    await shot('13-student-attempts', `${BASE}/student/attempts`);
    await shot('14-student-wrong', `${BASE}/student/wrong`);
    await shot('15-student-advice', `${BASE}/student/advice`);
    await shot('16-student-appeals', `${BASE}/student/appeals`);
    await shot('17-student-notifications', `${BASE}/student/notifications`);
    ws.close();
  } finally {
    chrome.kill('SIGTERM');
  }
}

try {
  await runApiSmoke();
  await runScreenshots();
  REPORT.finishedAt = new Date().toISOString();
  await fs.writeFile(path.join(OUT_DIR, 'test-report.json'), JSON.stringify(REPORT, null, 2));
  console.log(JSON.stringify(REPORT, null, 2));
} catch (error) {
  REPORT.error = error.stack || String(error);
  await fs.writeFile(path.join(OUT_DIR, 'test-report.json'), JSON.stringify(REPORT, null, 2));
  console.error(REPORT.error);
  process.exit(1);
}
