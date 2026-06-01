import { computed, reactive } from 'vue'

export function createPager(pageSize = 8) {
  return reactive({ page: 1, size: pageSize })
}

export function usePagedRows(rowsRef, pager) {
  return computed(() => {
    const rows = rowsRef.value || []
    const start = (pager.page - 1) * pager.size
    return rows.slice(start, start + pager.size)
  })
}

export function exportXls(filename, rows, columns) {
  const safeRows = rows || []
  const table = [
    '<table><thead><tr>',
    ...columns.map((col) => `<th>${escapeHtml(col.label)}</th>`),
    '</tr></thead><tbody>',
    ...safeRows.map((row) => `<tr>${columns.map((col) => `<td>${escapeHtml(readValue(row, col))}</td>`).join('')}</tr>`),
    '</tbody></table>'
  ].join('')
  const html = `\uFEFF<html><head><meta charset="UTF-8"></head><body>${table}</body></html>`
  const blob = new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${filename}.xls`
  link.click()
  URL.revokeObjectURL(link.href)
}

export function formatDateTime(value, fallback = '-') {
  if (!value) return fallback
  const text = String(value)
    .replace('T', ' ')
    .replace(/\.\d+$/, '')
  return text.length >= 19 ? text.slice(0, 19) : text
}

export function formatDateTimeMinute(value, fallback = '-') {
  const text = formatDateTime(value, fallback)
  return text === fallback ? text : text.slice(0, 16)
}

export function examStatusName(status) {
  return {
    IN_PROGRESS: '进行中',
    SUBMITTED: '已提交',
    PENDING_REVIEW: '待复核',
    TIMEOUT: '已超时',
    EXPIRED: '已过期',
    CANCELLED: '已取消'
  }[status] || status || '-'
}

export function reviewStatusName(status) {
  return {
    PENDING: '待复核',
    DONE: '已完成',
    AUTO: '自动评分',
    REVIEWED: '已阅卷',
    APPROVED: '通过',
    DRAFT: '草稿',
    REJECTED: '驳回'
  }[status] || status || '-'
}

export function appealStatusName(status) {
  return {
    PENDING: '待处理',
    APPROVED: '已同意',
    REJECTED: '已拒绝'
  }[status] || status || '-'
}

export function backupStatusName(status) {
  return {
    CREATED: '已创建',
    RESTORED: '已恢复'
  }[status] || status || '-'
}

export function roleName(role) {
  return {
    ADMIN: '管理员',
    TEACHER: '教师',
    STUDENT: '学生',
    SYSTEM: '系统'
  }[role] || role || '-'
}

export function monitorEventName(eventType) {
  return {
    START: '开始考试',
    SUBMIT: '提交考试',
    TIMEOUT: '到时自动提交',
    BLUR: '窗口失焦',
    VISIBILITY_CHANGE: '切换到后台',
    FULLSCREEN_ENTER: '进入全屏',
    FULLSCREEN_EXIT: '退出全屏',
    FULLSCREEN_FAIL: '全屏失败'
  }[eventType] || eventType || '-'
}

export function operationTargetName(target) {
  if (!target) return '-'
  const text = String(target)
  const match = text.match(/^([A-Za-z]+):(.+)$/)
  if (!match) return roleName(text)
  const [, type, id] = match
  const name = {
    attempt: '考试记录',
    paper: '试卷',
    question: '题目',
    answer: '答题记录',
    appeal: '申诉记录',
    user: '用户',
    course: '课程',
    class: '班级',
    teacherCourse: '授课安排',
    announcement: '公告',
    backup: '备份记录'
  }[type] || type
  return `${name} #${id}`
}

export function operationDetailName(detail) {
  if (detail == null || detail === '') return '-'
  const text = String(detail)
  const eventMatch = text.match(/^([A-Z_]+):(.*)$/)
  if (eventMatch) {
    const eventName = monitorEventName(eventMatch[1])
    const rest = operationDetailName(eventMatch[2])
    return rest === '-' || rest === eventName ? eventName : `${eventName}：${rest}`
  }
  if (/^score=/.test(text)) return `得分：${text.replace(/^score=/, '')}`
  if (/^paper:/.test(text)) return operationTargetName(text)
  if (/^attempt:/.test(text)) return operationTargetName(text)
  if (/^answer:/.test(text)) return operationTargetName(text)
  if (/^appeal:/.test(text)) return operationTargetName(text)
  return appealStatusName(text) !== text
    ? appealStatusName(text)
    : examStatusName(text) !== text
      ? examStatusName(text)
      : reviewStatusName(text) !== text
        ? reviewStatusName(text)
        : roleName(text)
}

export function systemTextName(value) {
  if (value == null) return ''
  const text = cleanGeneratedSuffix(String(value))
  return text
    .replaceAll('VISIBILITY_CHANGE', monitorEventName('VISIBILITY_CHANGE'))
    .replaceAll('FULLSCREEN_ENTER', monitorEventName('FULLSCREEN_ENTER'))
    .replaceAll('FULLSCREEN_EXIT', monitorEventName('FULLSCREEN_EXIT'))
    .replaceAll('FULLSCREEN_FAIL', monitorEventName('FULLSCREEN_FAIL'))
    .replaceAll('PENDING_REVIEW', examStatusName('PENDING_REVIEW'))
    .replaceAll('IN_PROGRESS', examStatusName('IN_PROGRESS'))
    .replaceAll('SUBMITTED', examStatusName('SUBMITTED'))
    .replaceAll('TIMEOUT', monitorEventName('TIMEOUT'))
    .replaceAll('SUBMIT', monitorEventName('SUBMIT'))
    .replaceAll('START', monitorEventName('START'))
    .replaceAll('BLUR', monitorEventName('BLUR'))
    .replaceAll('APPROVED', appealStatusName('APPROVED'))
    .replaceAll('REJECTED', appealStatusName('REJECTED'))
}

export function cleanGeneratedSuffix(value) {
  if (value == null) return ''
  return String(value).replace(/\s+\d{10,14}(?=$|[-—])/g, '')
}

function readValue(row, col) {
  if (col.formatter) return col.formatter(row)
  return col.prop?.split('.').reduce((obj, key) => obj?.[key], row) ?? ''
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
}
