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
