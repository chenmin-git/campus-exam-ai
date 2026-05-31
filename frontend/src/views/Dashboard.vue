<template>
  <div class="page dashboard-page">
    <div class="dashboard-head">
      <div>
        <span class="role-pill">{{ roleName }}</span>
        <h2 class="section-title">{{ roleCopy.title }}</h2>
        <p>{{ roleCopy.subtitle }}</p>
      </div>
      <div class="head-actions">
        <router-link v-for="action in quickActions" :key="action.path" :to="action.path" class="quick-link">
          <el-icon><component :is="action.icon" /></el-icon>
          <span>{{ action.label }}</span>
        </router-link>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <div v-for="item in metricCards" :key="item.label" class="metric-card">
        <div class="metric-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </div>
    </div>

    <div class="insight-grid">
      <div v-for="item in insightCards" :key="item.label" class="insight-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.hint }}</em>
      </div>
    </div>

    <div v-if="auth.user?.role === 'ADMIN'" class="dashboard-layout admin-layout">
      <section class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h3>考试运行概览</h3>
            <span>最近考试成绩记录</span>
          </div>
          <strong>{{ dashboard.averageScore || 0 }}</strong>
        </div>
        <div class="score-line compact">
          <div v-for="row in recentScores" :key="row.id" class="score-point" :style="{ height: scoreHeight(row.score) }">
            <span>{{ row.score || 0 }}</span>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>题型分布</h3>
            <span>平台题库结构</span>
          </div>
        </div>
        <div class="donut-wrap">
          <div class="donut" :style="{ background: donutBackground }"><span>{{ dashboard.questionCount || 0 }}</span></div>
          <div class="legend">
            <div v-for="item in questionTypeRows" :key="item.label">
              <i :style="{ background: item.color }"></i>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>
      </section>
      <section class="panel full-row">
        <div class="panel-head">
          <div>
            <h3>课程题量</h3>
            <span>按课程统计题库覆盖情况</span>
          </div>
        </div>
        <div class="bar-list">
          <div v-for="item in dashboard.courseQuestions || []" :key="item.name" class="bar-row">
            <span>{{ item.name }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: barWidth(item.count) }"></div></div>
            <strong>{{ item.count }}</strong>
          </div>
        </div>
      </section>
    </div>

    <div v-else-if="auth.user?.role === 'TEACHER'" class="dashboard-layout teacher-layout">
      <section class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h3>学生最近提交</h3>
            <span>我发布试卷的成绩反馈</span>
          </div>
          <strong>{{ dashboard.averageScore || 0 }}</strong>
        </div>
        <div class="score-line">
          <div v-for="row in recentScores" :key="row.id" class="score-point" :style="{ height: scoreHeight(row.score) }">
            <span>{{ row.score || 0 }}</span>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>我的题型结构</h3>
            <span>仅统计本人创建题目</span>
          </div>
        </div>
        <div class="donut-wrap">
          <div class="donut" :style="{ background: donutBackground }"><span>{{ dashboard.questionCount || 0 }}</span></div>
          <div class="legend">
            <div v-for="item in questionTypeRows" :key="item.label">
              <i :style="{ background: item.color }"></i>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>我的课程题量</h3>
            <span>按课程统计</span>
          </div>
        </div>
        <div class="bar-list">
          <div v-for="item in dashboard.courseQuestions || []" :key="item.name" class="bar-row">
            <span>{{ item.name }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: barWidth(item.count) }"></div></div>
            <strong>{{ item.count }}</strong>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>试卷状态</h3>
            <span>发布情况</span>
          </div>
        </div>
        <div class="status-grid">
          <div v-for="item in paperStatusRows" :key="item.label">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </section>
    </div>

    <div v-else class="dashboard-layout student-layout">
      <section class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h3>我的成绩走势</h3>
            <span>个人历史考试记录</span>
          </div>
          <strong>{{ dashboard.averageScore || 0 }}</strong>
        </div>
        <div class="score-line">
          <div v-for="row in studentScoreTrend" :key="row.id" class="score-point" :style="{ height: scoreHeight(row.score) }">
            <span>{{ row.score || 0 }}</span>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>学习进度</h3>
            <span>只展示个人学习数据</span>
          </div>
        </div>
        <div class="bar-list">
          <div v-for="item in dashboard.studyProgress || []" :key="item.name" class="bar-row">
            <span>{{ item.name }}</span>
            <div class="bar-track"><div class="bar-fill" :style="{ width: studentProgressWidth(item.count) }"></div></div>
            <strong>{{ item.count }}</strong>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>学习提醒</h3>
            <span>接下来优先处理</span>
          </div>
        </div>
        <div class="todo-list">
          <router-link to="/student/exams">当前可参加考试 <strong>{{ dashboard.availableExamCount || 0 }}</strong></router-link>
          <router-link to="/student/wrong">待复盘错题 <strong>{{ dashboard.wrongCount || 0 }}</strong></router-link>
          <router-link to="/student/attempts">个人最高分 <strong>{{ dashboard.bestScore || 0 }}</strong></router-link>
        </div>
      </section>
      <section class="panel">
        <div class="panel-head">
          <div>
            <h3>薄弱知识点</h3>
            <span>根据错题自动归纳</span>
          </div>
        </div>
        <div v-if="weakKnowledgeRows.length" class="knowledge-list">
          <div v-for="item in weakKnowledgeRows" :key="item.knowledgePoint">
            <span>{{ item.knowledgePoint }}</span>
            <strong>{{ item.wrongCount }} 题</strong>
          </div>
        </div>
        <el-empty v-else description="暂无错题薄弱项" />
      </section>
    </div>

    <section class="panel announcement-panel">
      <div class="panel-head">
        <div>
          <h3>公告通知</h3>
          <span>近期考试与系统消息</span>
        </div>
        <el-button @click="exportAnnouncements">导出xls</el-button>
      </div>
      <el-table :data="pagedAnnouncements" size="small">
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
      </el-table>
      <div class="table-actions">
        <el-pagination v-model:current-page="announcementPager.page" v-model:page-size="announcementPager.size" :total="announcements.length" :page-sizes="[5,8,12]" layout="total, prev, pager, next" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  Bell,
  Collection,
  DataAnalysis,
  Document,
  EditPen,
  Medal,
  Notebook,
  Refresh,
  Tickets,
  TrendCharts,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import { createPager, exportXls, usePagedRows } from '../utils/tableTools'

const auth = useAuthStore()
const announcements = ref([])
const dashboard = ref({})
const announcementPager = createPager(5)
const announcementRows = computed(() => announcements.value)
const pagedAnnouncements = usePagedRows(announcementRows, announcementPager)
const colors = ['#00a1d6', '#fb7299', '#ffd36e', '#56c2a8', '#7c3aed']
const typeNames = { SINGLE: '单选题', MULTIPLE: '多选题', JUDGE: '判断题', SHORT: '简答题', PROGRAM: '编程题' }
const roleName = computed(() => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[auth.user?.role] || '用户'))
const roleCopy = computed(() => {
  if (auth.user?.role === 'TEACHER') {
    return { title: '教学工作台', subtitle: '题库建设、试卷发布、学生成绩反馈集中查看' }
  }
  if (auth.user?.role === 'STUDENT') {
    return { title: '学习工作台', subtitle: '查看考试安排、学习成绩和错题解析' }
  }
  return { title: '系统工作台', subtitle: '考试运行、用户规模、课程与题库概览' }
})

const quickActions = computed(() => {
  if (auth.user?.role === 'TEACHER') {
    return [
      { label: '题库管理', path: '/teacher/questions', icon: EditPen },
      { label: '试卷管理', path: '/teacher/papers', icon: Tickets },
      { label: '成绩管理', path: '/teacher/scores', icon: TrendCharts }
    ]
  }
  if (auth.user?.role === 'STUDENT') {
    return [
      { label: '在线考试', path: '/student/exams', icon: Document },
      { label: '历史成绩', path: '/student/attempts', icon: DataAnalysis },
      { label: '错题解析', path: '/student/wrong', icon: Medal }
    ]
  }
  return [
    { label: '用户管理', path: '/admin/users', icon: User },
    { label: '课程管理', path: '/admin/courses', icon: Notebook },
    { label: '权限管理', path: '/admin/permissions', icon: Collection }
  ]
})

const metricCards = computed(() => [
  ...(auth.user?.role === 'STUDENT'
    ? [
        { label: '可参加考试', value: dashboard.value.availableExamCount || 0, hint: '已发布试卷', icon: Document },
        { label: '已完成考试', value: dashboard.value.finishedCount || 0, hint: '个人完成记录', icon: Tickets },
        { label: '待复盘错题', value: dashboard.value.wrongCount || 0, hint: '建议优先查看 AI 解析', icon: Medal },
        { label: '个人均分', value: dashboard.value.averageScore || 0, hint: `最高分 ${dashboard.value.bestScore || 0}`, icon: TrendCharts }
      ]
    : auth.user?.role === 'TEACHER'
      ? [
          { label: '我的题目', value: dashboard.value.questionCount || 0, hint: '本人创建题目', icon: EditPen },
          { label: '我的试卷', value: dashboard.value.paperCount || 0, hint: `已发布 ${dashboard.value.publishedPaperCount || 0}`, icon: Tickets },
          { label: '学生提交', value: dashboard.value.attemptCount || 0, hint: '我发布试卷的记录', icon: UserFilled },
          { label: '试卷均分', value: dashboard.value.averageScore || 0, hint: '用于教学反馈', icon: TrendCharts }
        ]
      : [
          { label: '用户总数', value: dashboard.value.userCount || 0, hint: `学生 ${dashboard.value.studentCount || 0} · 教师 ${dashboard.value.teacherCount || 0}`, icon: User },
          { label: '课程数量', value: dashboard.value.courseCount || 0, hint: '已维护课程', icon: Notebook },
          { label: '题库题量', value: dashboard.value.questionCount || 0, hint: `主观题 ${dashboard.value.subjectiveQuestionCount || 0}`, icon: EditPen },
          { label: '考试记录', value: dashboard.value.attemptCount || 0, hint: `均分 ${dashboard.value.averageScore || 0}`, icon: DataAnalysis }
        ])
])

const insightCards = computed(() => {
  if (auth.user?.role === 'STUDENT') {
    return [
      { label: '当前最高分', value: dashboard.value.bestScore || 0, hint: '历史最佳表现' },
      { label: '待复核记录', value: dashboard.value.pendingReviewCount || 0, hint: '主观题阅卷中' },
      { label: '重点复习', value: weakKnowledgeRows.value[0]?.knowledgePoint || '暂无', hint: weakKnowledgeRows.value[0] ? `${weakKnowledgeRows.value[0].wrongCount} 道错题` : '继续保持' }
    ]
  }
  if (auth.user?.role === 'TEACHER') {
    return [
      { label: '待人工阅卷', value: dashboard.value.pendingReviewCount || 0, hint: '需要教师处理' },
      { label: '待处理申诉', value: dashboard.value.pendingAppealCount || 0, hint: '学生复查申请' },
      { label: '覆盖班级', value: (dashboard.value.classCoverage || []).length, hint: classCoverageHint.value }
    ]
  }
  return [
    { label: '班级规模', value: dashboard.value.classCount || 0, hint: '已维护班级' },
    { label: '发布试卷', value: dashboard.value.publishedPaperCount || 0, hint: `总试卷 ${dashboard.value.paperCount || 0}` },
    { label: '待复核考试', value: dashboard.value.pendingReviewCount || 0, hint: '主观题待阅卷' }
  ]
})

const questionTypeRows = computed(() => Object.entries(dashboard.value.questionTypes || {}).map(([key, value], index) => ({
  label: typeNames[key] || key,
  value,
  color: colors[index % colors.length]
})))

const donutBackground = computed(() => {
  const total = questionTypeRows.value.reduce((sum, item) => sum + item.value, 0)
  if (!total) return '#e5e7eb'
  let current = 0
  const parts = questionTypeRows.value.map((item) => {
    const start = current
    current += (item.value / total) * 100
    return `${item.color} ${start}% ${current}%`
  })
  return `conic-gradient(${parts.join(', ')})`
})

const recentScores = computed(() => (dashboard.value.recentScores || []).slice().reverse())
const studentScoreTrend = computed(() => (dashboard.value.scoreTrend || []).slice().reverse())
const paperStatusRows = computed(() => Object.entries(dashboard.value.paperStatus || {}).map(([label, value]) => ({ label, value })))
const maxCourseCount = computed(() => Math.max(1, ...(dashboard.value.courseQuestions || []).map((item) => item.count)))
const maxStudyCount = computed(() => Math.max(1, ...(dashboard.value.studyProgress || []).map((item) => item.count)))
const weakKnowledgeRows = computed(() => dashboard.value.weakKnowledgePoints || [])
const classCoverageHint = computed(() => {
  const rows = dashboard.value.classCoverage || []
  if (!rows.length) return '暂无授课绑定'
  return rows.slice(0, 2).map((row) => `${row.className}·${row.courseName}`).join('；')
})

function scoreHeight(score) {
  return `${Math.max(10, score || 0)}%`
}

function barWidth(count) {
  return `${Math.max(6, (count / maxCourseCount.value) * 100)}%`
}

function studentProgressWidth(count) {
  return `${Math.max(6, (count / maxStudyCount.value) * 100)}%`
}

async function load() {
  const [announcementData, dashboardData] = await Promise.all([
    http.get('/common/announcements'),
    http.get('/common/dashboard')
  ])
  announcements.value = announcementData
  dashboard.value = dashboardData
}

function exportAnnouncements() {
  exportXls('工作台公告', announcements.value, [
    { label: '标题', prop: 'title' },
    { label: '内容', prop: 'content' }
  ])
}

onMounted(load)
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 14px;
}

.dashboard-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  padding: 18px 20px;
  border: 1px solid #d9eef9;
  border-radius: 14px;
  background:
    linear-gradient(135deg, rgba(0, 161, 214, .12), rgba(251, 114, 153, .08)),
    #fff;
  box-shadow: 0 12px 30px rgba(31, 42, 68, .06);
}

.dashboard-head p {
  margin: 4px 0 0;
  color: var(--muted);
}

.role-pill {
  display: inline-flex;
  margin-bottom: 8px;
  padding: 3px 10px;
  border-radius: 999px;
  color: var(--accent);
  background: #eef9ff;
  font-size: 12px;
  font-weight: 800;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.quick-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid #d5edf8;
  border-radius: 8px;
  color: #155a78;
  background: rgba(255, 255, 255, .72);
  text-decoration: none;
  font-weight: 700;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  display: flex;
  gap: 12px;
  min-height: 108px;
  padding: 16px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(31, 42, 68, .06);
}

.metric-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
}

.metric-card span,
.metric-card em {
  display: block;
  color: var(--muted);
  font-style: normal;
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin: 5px 0 2px;
  color: #10243a;
  font-size: 30px;
  line-height: 1;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.insight-card {
  min-height: 86px;
  padding: 14px 16px;
  border: 1px solid #dfeaf6;
  border-left: 4px solid var(--accent);
  border-radius: 10px;
  background: rgba(255, 255, 255, .86);
  box-shadow: 0 8px 22px rgba(31, 42, 68, .05);
}

.insight-card span,
.insight-card em {
  display: block;
  color: var(--muted);
  font-size: 13px;
  font-style: normal;
}

.insight-card strong {
  display: block;
  margin: 5px 0;
  color: #10243a;
  font-size: 22px;
  line-height: 1.2;
  word-break: break-word;
}

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, .75fr);
  gap: 14px;
}

.teacher-layout,
.student-layout {
  grid-template-columns: minmax(0, 1.25fr) minmax(340px, .8fr);
}

.full-row {
  grid-column: 1 / -1;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h3 {
  margin: 0 0 4px;
  font-size: 17px;
}

.panel-head span {
  color: var(--muted);
  font-size: 13px;
}

.panel-head > strong {
  color: var(--accent);
  font-size: 30px;
}

.chart-panel {
  min-height: 300px;
}

.donut-wrap {
  display: flex;
  align-items: center;
  gap: 22px;
  min-height: 190px;
}

.donut {
  width: 144px;
  height: 144px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  position: relative;
  flex: 0 0 auto;
}

.donut::after {
  content: "";
  position: absolute;
  inset: 30px;
  border-radius: 50%;
  background: #fff;
}

.donut span {
  position: relative;
  z-index: 1;
  font-size: 26px;
  font-weight: 800;
}

.legend {
  display: grid;
  gap: 10px;
  flex: 1;
}

.legend div {
  display: grid;
  grid-template-columns: 12px 1fr auto;
  align-items: center;
  gap: 10px;
}

.legend i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.bar-list {
  display: grid;
  gap: 14px;
}

.bar-row {
  display: grid;
  grid-template-columns: 120px 1fr 40px;
  align-items: center;
  gap: 12px;
}

.bar-track {
  height: 12px;
  background: #edf2f7;
  border-radius: 999px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--accent), var(--accent-2));
}

.score-line {
  height: 220px;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 26px 0 8px;
  border-bottom: 1px solid var(--line);
}

.score-line.compact {
  height: 190px;
}

.score-point {
  flex: 1;
  min-width: 24px;
  max-width: 80px;
  background: linear-gradient(180deg, var(--accent-2), var(--accent));
  border-radius: 8px 8px 0 0;
  position: relative;
}

.score-point span {
  position: absolute;
  top: -22px;
  left: 50%;
  transform: translateX(-50%);
  color: var(--muted);
  font-size: 12px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.status-grid div,
.todo-list a {
  padding: 16px;
  border-radius: 12px;
  background: #f5fbff;
  border: 1px solid #dff3ff;
}

.status-grid strong {
  display: block;
  color: var(--accent);
  font-size: 28px;
}

.todo-list {
  display: grid;
  gap: 10px;
}

.todo-list a {
  display: flex;
  justify-content: space-between;
  color: #24364b;
  text-decoration: none;
}

.todo-list strong {
  color: var(--accent);
}

.knowledge-list {
  display: grid;
  gap: 10px;
}

.knowledge-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #dff3ff;
  border-radius: 10px;
  background: #f5fbff;
}

.knowledge-list span {
  color: #24364b;
  font-weight: 700;
}

.knowledge-list strong {
  color: var(--accent);
  white-space: nowrap;
}

.announcement-panel {
  margin-bottom: 8px;
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .insight-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-layout,
  .teacher-layout,
  .student-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .head-actions,
  .quick-link,
  .head-actions .el-button {
    width: 100%;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .bar-row {
    grid-template-columns: 96px 1fr 34px;
  }

  .donut-wrap {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
