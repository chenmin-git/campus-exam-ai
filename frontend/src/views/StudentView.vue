<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">学生考试中心</h2>
        <div class="muted">在线考试、成绩记录、错题 AI 解析和个人资料</div>
      </div>
      <el-tag v-if="current" type="danger" size="large">考试中 · {{ remainingText }}</el-tag>
    </div>

    <el-tabs v-model="active" class="route-tabs">
      <el-tab-pane label="在线考试" name="exams">
        <div class="grid">
          <div v-for="paper in papers" :key="paper.id" class="panel exam-card">
            <div class="paper-head">
              <h3>{{ paper.title }}</h3>
              <el-tag :type="attemptedPaperIds.includes(paper.id) ? 'info' : 'success'">{{ attemptedPaperIds.includes(paper.id) ? '已参加' : '可考试' }}</el-tag>
            </div>
            <p class="muted">时长 {{ paper.durationMinutes }} 分钟 · 总分 {{ paper.totalScore }} · 课程ID {{ paper.courseId }}</p>
            <p class="muted">开始 {{ paper.startTime || '不限' }} · 结束 {{ paper.endTime || '不限' }} · {{ paper.allowRetake ? '允许重考' : '禁止重考' }}</p>
            <el-button type="primary" @click="confirmStart(paper)">开始考试</el-button>
          </div>
        </div>

        <div v-if="current" class="panel exam">
          <div class="toolbar">
            <div>
              <h2 class="section-title">{{ current.paper.title }}</h2>
              <div class="muted">已作答 {{ answeredCount }} / {{ current.questions.length }}</div>
            </div>
            <div class="exam-head-actions">
              <el-progress type="circle" :width="72" :percentage="answerPercent" />
              <el-button @click="enterFullscreen">全屏</el-button>
            </div>
          </div>
          <div v-for="(q, index) in current.questions" :key="q.id" class="question">
            <h3>{{ index + 1 }}. {{ q.stem }}（{{ q.score }}分）</h3>
            <el-checkbox-group v-if="q.type === 'MULTIPLE'" v-model="answers[q.id]">
              <el-checkbox v-for="op in q.options" :key="op.id" :value="op.optionKey">{{ op.optionKey }}. {{ op.optionText }}</el-checkbox>
            </el-checkbox-group>
            <el-radio-group v-else-if="q.type === 'SINGLE' || q.type === 'JUDGE'" v-model="answers[q.id]">
              <el-radio v-for="op in q.options" :key="op.id" :value="op.optionKey">{{ op.optionKey }}. {{ op.optionText }}</el-radio>
            </el-radio-group>
            <el-input v-else v-model="answers[q.id]" type="textarea" :rows="4" placeholder="请输入简答/编程答案" />
          </div>
          <div class="submit-bar">
            <el-button @click="current=null">暂不提交</el-button>
            <el-button type="success" @click="submitExam">提交试卷</el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="历史成绩" name="attempts">
        <div class="toolbar">
          <span class="muted">共 {{ attempts.length }} 条考试记录</span>
          <el-button @click="exportAttempts">导出xls</el-button>
        </div>
        <el-table :data="pagedAttempts" class="panel" stripe>
          <el-table-column prop="paperId" label="试卷ID" width="100" />
          <el-table-column prop="score" label="成绩" width="100"><template #default="{ row }"><strong>{{ row.score }}</strong></template></el-table-column>
          <el-table-column prop="status" label="状态" width="130" />
          <el-table-column prop="startedAt" label="开始时间" />
          <el-table-column prop="submittedAt" label="提交时间" />
        </el-table>
        <div class="table-actions"><el-pagination v-model:current-page="attemptPager.page" v-model:page-size="attemptPager.size" :total="attempts.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      </el-tab-pane>

      <el-tab-pane label="错题解析" name="wrong">
        <el-empty v-if="wrongQuestions.length === 0" description="暂无错题记录" />
        <div v-for="item in wrongQuestions" :key="item.attemptId + '-' + item.question.id" class="panel wrong">
          <div class="toolbar">
            <h3>{{ item.question.stem }}</h3>
            <el-tag>{{ typeName(item.question.type) }}</el-tag>
          </div>
          <p class="muted">你的答案：{{ item.studentAnswer || '未作答' }}；正确答案：{{ item.question.correctAnswer }}</p>
          <el-descriptions border :column="1">
            <el-descriptions-item label="原始解析">{{ item.question.analysis || '暂无人工解析' }}</el-descriptions-item>
          </el-descriptions>
          <el-button class="ai-btn" :loading="aiLoading === item.question.id" @click="analyze(item)">AI解析</el-button>
          <pre v-if="analysis[item.question.id]">{{ analysis[item.question.id] }}</pre>
        </div>
      </el-tab-pane>

      <el-tab-pane label="学习建议" name="advice">
        <div class="toolbar">
          <span class="muted">根据错题生成个性化复习建议</span>
          <el-button @click="loadAdvice">刷新</el-button>
        </div>
        <div class="panel advice-grid">
          <div v-for="item in adviceRows" :key="item.knowledgePoint" class="advice-item">
            <strong>{{ item.knowledgePoint }}</strong>
            <span>错题 {{ item.wrongCount }} 道</span>
            <p>{{ item.advice }}</p>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="成绩申诉" name="appeals">
        <div class="toolbar">
          <span class="muted">对已完成考试成绩发起复查申请</span>
          <el-button @click="loadAppeals">刷新</el-button>
        </div>
        <div class="panel">
          <el-form :model="appealForm" inline>
            <el-form-item label="考试记录">
              <el-select v-model="appealForm.attemptId" placeholder="选择考试记录">
                <el-option v-for="item in attempts" :key="item.id" :label="`${item.paperId} · ${item.score}`" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="原因"><el-input v-model="appealForm.reason" style="width: 360px" /></el-form-item>
            <el-form-item><el-button type="primary" @click="submitAppeal">提交申诉</el-button></el-form-item>
          </el-form>
        </div>
        <el-table :data="appealRows" class="panel" stripe>
          <el-table-column prop="attemptId" label="考试记录" width="100" />
          <el-table-column prop="reason" label="原因" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="reply" label="回复" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="通知中心" name="notifications">
        <div class="toolbar">
          <span class="muted">考试、复核和系统通知</span>
          <el-button @click="loadNotifications">刷新</el-button>
        </div>
        <el-table :data="notifications" class="panel" stripe>
          <el-table-column prop="title" label="标题" width="180" />
          <el-table-column prop="content" label="内容" />
          <el-table-column prop="readFlag" label="状态" width="90">
            <template #default="{ row }"><el-tag :type="row.readFlag ? 'info' : 'success'">{{ row.readFlag ? '已读' : '未读' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="markNotification(row)">已读</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="个人中心" name="profile">
        <div class="profile-grid">
          <div class="panel">
            <h3>基本信息</h3>
            <el-form :model="profile" label-width="80px">
              <el-form-item label="姓名"><el-input v-model="profile.realName" /></el-form-item>
              <el-form-item label="电话"><el-input v-model="profile.phone" /></el-form-item>
              <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
              <el-form-item><el-button type="primary" @click="saveProfile">保存资料</el-button></el-form-item>
            </el-form>
          </div>
          <div class="panel">
            <h3>修改密码</h3>
            <el-form :model="passwordForm" label-width="80px">
              <el-form-item label="原密码"><el-input v-model="passwordForm.oldPassword" type="password" show-password /></el-form-item>
              <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item>
              <el-form-item><el-button type="primary" @click="changePassword">更新密码</el-button></el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { createPager, exportXls, usePagedRows } from '../utils/tableTools'

const route = useRoute()
const router = useRouter()
const active = ref(route.params.section || 'exams')
const papers = ref([])
const attempts = ref([])
const wrongQuestions = ref([])
const adviceRows = ref([])
const appealRows = ref([])
const notifications = ref([])
const current = ref(null)
const answers = reactive({})
const profile = reactive({})
const passwordForm = reactive({ oldPassword: '', newPassword: '' })
const appealForm = reactive({ attemptId: null, reason: '' })
const now = ref(Date.now())
const timer = setInterval(() => (now.value = Date.now()), 1000)
const analysis = reactive({})
const aiLoading = ref(null)
const attemptPager = createPager()
const attemptRows = computed(() => attempts.value)
const pagedAttempts = usePagedRows(attemptRows, attemptPager)

const attemptedPaperIds = computed(() => attempts.value.map((a) => a.paperId))
const answeredCount = computed(() => current.value ? current.value.questions.filter((q) => Array.isArray(answers[q.id]) ? answers[q.id].length : !!answers[q.id]).length : 0)
const answerPercent = computed(() => current.value ? Math.round((answeredCount.value / current.value.questions.length) * 100) : 0)
const remainingSeconds = computed(() => {
  if (!current.value?.attempt?.dueAt) return null
  return Math.max(0, Math.floor((new Date(current.value.attempt.dueAt).getTime() - now.value) / 1000))
})
const remainingText = computed(() => {
  if (remainingSeconds.value == null) return '--:--'
  const seconds = remainingSeconds.value
  const min = String(Math.floor(seconds / 60)).padStart(2, '0')
  const sec = String(seconds % 60).padStart(2, '0')
  return `${min}:${sec}`
})

async function load() {
  const [paperData, attemptData, wrongData, profileData, adviceData, appealData, notificationData] = await Promise.all([
    http.get('/student/exams'),
    http.get('/student/attempts'),
    http.get('/student/wrong-questions'),
    http.get('/student/profile'),
    http.get('/student/study-advice'),
    http.get('/student/appeals'),
    http.get('/student/notifications')
  ])
  papers.value = paperData
  attempts.value = attemptData
  wrongQuestions.value = wrongData
  Object.assign(profile, profileData)
  adviceRows.value = adviceData
  appealRows.value = appealData
  notifications.value = notificationData
}

async function confirmStart(paper) {
  await ElMessageBox.confirm(`确认开始《${paper.title}》？开始后系统会创建考试记录。`, '开始考试', { type: 'info' })
  await startExam(paper.id)
}

async function startExam(id) {
  current.value = await http.post(`/student/exams/${id}/start`)
  Object.keys(answers).forEach((key) => delete answers[key])
  current.value.questions.forEach((q) => {
    answers[q.id] = q.type === 'MULTIPLE' ? [] : ''
  })
  await recordMonitor('START', '开始考试')
}

async function submitExam() {
  if (!current.value) return
  await ElMessageBox.confirm(`当前已作答 ${answeredCount.value}/${current.value.questions.length} 题，确认提交吗？`, '提交试卷', { type: 'warning' })
  await submitExamPayload()
}

async function submitExamPayload() {
  if (!current.value) return
  const payload = current.value.questions.map((q) => ({
    questionId: q.id,
    answer: Array.isArray(answers[q.id]) ? answers[q.id].join(',') : answers[q.id]
  }))
  const result = await http.post(`/student/attempts/${current.value.attempt.id}/submit`, { answers: payload })
  ElMessage.success(`提交成功，得分 ${result.score}`)
  await recordMonitor('SUBMIT', '主动提交试卷')
  current.value = null
  router.push('/student/attempts')
  load()
}

async function autoSubmitExam() {
  if (!current.value) return
  await recordMonitor('TIMEOUT', '考试到时自动提交')
  await submitExamPayload()
}

async function analyze(item) {
  aiLoading.value = item.question.id
  try {
    analysis[item.question.id] = await http.post(`/student/wrong-questions/${item.question.id}/ai-analysis`, {
      studentAnswer: item.studentAnswer
    })
  } finally {
    aiLoading.value = null
  }
}

async function saveProfile() {
  const data = await http.post('/student/profile', profile)
  Object.assign(profile, data)
  ElMessage.success('资料已保存')
}

async function changePassword() {
  await http.post('/student/password', passwordForm)
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  ElMessage.success('密码已更新')
}

async function loadAdvice() {
  adviceRows.value = await http.get('/student/study-advice')
}

async function loadAppeals() {
  appealRows.value = await http.get('/student/appeals')
}

async function submitAppeal() {
  await http.post('/student/appeals', appealForm)
  ElMessage.success('申诉已提交')
  appealForm.reason = ''
  loadAppeals()
}

async function loadNotifications() {
  notifications.value = await http.get('/student/notifications')
}

async function markNotification(row) {
  await http.post(`/student/notifications/${row.id}/read`)
  await loadNotifications()
}

async function recordMonitor(eventType, detail) {
  if (!current.value?.attempt?.id) return
  try {
    await http.post('/student/monitor', {
      attemptId: current.value.attempt.id,
      eventType,
      detail
    })
  } catch (_) {
    // ignore monitoring failures
  }
}

async function enterFullscreen() {
  try {
    const element = document.documentElement
    if (element.requestFullscreen && !document.fullscreenElement) {
      await element.requestFullscreen()
    }
    await recordMonitor('FULLSCREEN_ENTER', '进入全屏考试')
  } catch (_) {
    await recordMonitor('FULLSCREEN_FAIL', '全屏失败')
  }
}

function typeName(type) {
  return { SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断', SHORT: '简答', PROGRAM: '编程' }[type] || type
}

function exportAttempts() {
  exportXls('历史成绩', attempts.value, [
    { label: '试卷ID', prop: 'paperId' },
    { label: '成绩', prop: 'score' },
    { label: '状态', prop: 'status' },
    { label: '开始时间', prop: 'startedAt' },
    { label: '提交时间', prop: 'submittedAt' }
  ])
}

onMounted(load)
watch(remainingSeconds, (seconds) => {
  if (current.value && seconds === 0) {
    autoSubmitExam().catch(() => {})
  }
})
const handleVisibility = () => {
  if (document.hidden && current.value) {
    recordMonitor('VISIBILITY_CHANGE', '切换到后台')
  }
}
const handleBlur = () => {
  if (current.value) {
    recordMonitor('BLUR', '窗口失焦')
  }
}
const handleFullscreenChange = () => {
  if (current.value && !document.fullscreenElement) {
    recordMonitor('FULLSCREEN_EXIT', '退出全屏考试')
  }
}
document.addEventListener('visibilitychange', handleVisibility)
document.addEventListener('fullscreenchange', handleFullscreenChange)
window.addEventListener('blur', handleBlur)
watch(() => route.params.section, (section) => {
  active.value = section || 'exams'
})
onUnmounted(() => {
  clearInterval(timer)
  document.removeEventListener('visibilitychange', handleVisibility)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  window.removeEventListener('blur', handleBlur)
})
</script>

<style scoped>
.route-tabs :deep(.el-tabs__header) {
  display: none;
}

.exam-card h3,
.wrong h3 {
  margin: 0;
  font-size: 17px;
}

.paper-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.exam,
.wrong {
  margin-top: 16px;
}

.question {
  padding: 16px 0;
  border-top: 1px solid var(--line);
}

.question h3 {
  font-size: 16px;
}

.submit-bar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid var(--line);
  padding-top: 14px;
}

.exam-head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-btn {
  margin-top: 12px;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
  gap: 14px;
}

pre {
  white-space: pre-wrap;
  background: #f4f7f6;
  border: 1px solid #dce7e4;
  border-radius: 8px;
  padding: 12px;
  line-height: 1.7;
}

.advice-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.advice-item {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fbfdff;
}

.advice-item strong,
.advice-item span,
.advice-item p {
  display: block;
}

.advice-item p {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.6;
}
</style>
