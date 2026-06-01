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
