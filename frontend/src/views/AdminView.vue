<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2 class="section-title">系统管理</h2>
        <div class="muted">维护用户、班级、课程、授课关系和公告</div>
      </div>
      <el-input v-model="keyword" clearable placeholder="搜索名称、账号、编号" class="search" />
    </div>

    <el-tabs v-model="active" class="route-tabs">
      <el-tab-pane label="用户管理" name="users">
        <div class="toolbar">
          <el-segmented v-model="roleFilter" :options="roleFilterOptions" />
          <div><el-button @click="exportUsers">导出xls</el-button><el-button type="primary" @click="openUser()">新增用户</el-button></div>
        </div>
        <el-table :data="pagedUsers" class="panel" stripe>
          <el-table-column prop="username" label="账号" width="140" />
          <el-table-column prop="realName" label="姓名" width="140" />
          <el-table-column prop="role" label="角色" width="110">
            <template #default="{ row }"><el-tag>{{ roleName(row.role) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="phone" label="电话" width="150" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="enabled" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openUser(row)">编辑</el-button>
              <el-button link type="danger" @click="remove('/admin/users/', row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-actions"><el-pagination v-model:current-page="pagers.users.page" v-model:page-size="pagers.users.size" :total="filteredUsers.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      </el-tab-pane>

      <el-tab-pane label="班级管理" name="classes">
        <div class="toolbar">
          <span class="muted">共 {{ classes.length }} 个班级</span>
          <div><el-button @click="exportClasses">导出xls</el-button><el-button type="primary" @click="openClass()">新增班级</el-button></div>
        </div>
        <el-table :data="pagedClasses" class="panel" stripe>
          <el-table-column prop="name" label="班级" />
          <el-table-column prop="grade" label="年级" width="140" />
          <el-table-column prop="major" label="专业" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openClass(row)">编辑</el-button>
              <el-button link type="danger" @click="remove('/admin/classes/', row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-actions"><el-pagination v-model:current-page="pagers.classes.page" v-model:page-size="pagers.classes.size" :total="filteredClasses.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      </el-tab-pane>

      <el-tab-pane label="课程管理" name="courses">
        <div class="toolbar">
          <span class="muted">共 {{ courses.length }} 门课程</span>
          <div><el-button @click="exportCourses">导出xls</el-button><el-button type="primary" @click="openCourse()">新增课程</el-button></div>
        </div>
        <el-table :data="pagedCourses" class="panel" stripe>
          <el-table-column prop="code" label="编号" width="140" />
          <el-table-column prop="name" label="课程" width="180" />
          <el-table-column prop="description" label="简介" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openCourse(row)">编辑</el-button>
              <el-button link type="danger" @click="remove('/admin/courses/', row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-actions"><el-pagination v-model:current-page="pagers.courses.page" v-model:page-size="pagers.courses.size" :total="filteredCourses.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      </el-tab-pane>

      <el-tab-pane label="授课安排" name="teacher-courses">
        <div class="toolbar">
          <span class="muted">教师、课程、班级三方绑定</span>
          <div><el-button @click="exportTeacherCourses">导出xls</el-button><el-button type="primary" @click="openTeacherCourse()">新增安排</el-button></div>
        </div>
        <el-table :data="pagedTeacherCourses" class="panel" stripe>
          <el-table-column label="教师"><template #default="{ row }">{{ userName(row.teacherId) }}</template></el-table-column>
          <el-table-column label="课程"><template #default="{ row }">{{ courseName(row.courseId) }}</template></el-table-column>
          <el-table-column label="班级"><template #default="{ row }">{{ className(row.classId) }}</template></el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openTeacherCourse(row)">编辑</el-button>
              <el-button link type="danger" @click="remove('/admin/teacher-courses/', row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-actions"><el-pagination v-model:current-page="pagers.teacherCourses.page" v-model:page-size="pagers.teacherCourses.size" :total="teacherCourses.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      </el-tab-pane>

      <el-tab-pane label="公告管理" name="announcements">
        <div class="toolbar">
          <span class="muted">公告会展示在工作台</span>
          <div><el-button @click="exportAnnouncements">导出xls</el-button><el-button type="primary" @click="openAnnouncement()">发布公告</el-button></div>
        </div>
        <el-table :data="pagedAnnouncements" class="panel" stripe>
          <el-table-column prop="title" label="标题" width="220" />
          <el-table-column prop="content" label="内容" />
          <el-table-column prop="enabled" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '显示' : '隐藏' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openAnnouncement(row)">编辑</el-button>
              <el-button link type="danger" @click="remove('/admin/announcements/', row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="table-actions"><el-pagination v-model:current-page="pagers.announcements.page" v-model:page-size="pagers.announcements.size" :total="filteredAnnouncements.length" :page-sizes="[5,8,12,20]" layout="total, sizes, prev, pager, next" /></div>
      </el-tab-pane>

      <el-tab-pane label="权限管理" name="permissions">
        <div class="toolbar">
          <div class="muted">按角色分配左侧功能菜单权限</div>
          <el-button type="primary" @click="savePermissions">保存权限</el-button>
        </div>
        <div class="permission-grid">
          <div class="panel">
            <h3>角色</h3>
            <el-radio-group v-model="permissionRole" class="role-list" @change="loadRolePermissions">
              <el-radio-button value="ADMIN">管理员</el-radio-button>
              <el-radio-button value="TEACHER">教师</el-radio-button>
              <el-radio-button value="STUDENT">学生</el-radio-button>
            </el-radio-group>
          </div>
          <div class="panel">
            <h3>功能权限</h3>
            <el-checkbox-group v-model="checkedPermissions" class="permission-list">
              <div v-for="group in permissionGroups" :key="group.module" class="permission-group">
                <strong>{{ group.module }}</strong>
                <div class="permission-items">
                  <el-checkbox v-for="p in group.items" :key="p.code" :value="p.code">{{ p.name }}</el-checkbox>
                </div>
              </div>
            </el-checkbox-group>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <div class="toolbar">
          <span class="muted">记录关键业务操作</span>
          <el-button @click="loadLogs">刷新</el-button>
        </div>
        <el-table :data="logs" class="panel" stripe>
          <el-table-column prop="createdAt" label="时间" width="180" />
          <el-table-column prop="username" label="用户" width="140" />
          <el-table-column prop="role" label="角色" width="100" />
          <el-table-column prop="action" label="动作" width="140" />
          <el-table-column prop="target" label="目标" width="160" />
          <el-table-column prop="detail" label="详情" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="备份恢复" name="backup">
        <div class="toolbar">
          <span class="muted">逻辑备份与恢复记录</span>
          <div><el-button @click="createBackup">创建备份记录</el-button><el-button @click="loadBackups">刷新</el-button></div>
        </div>
        <el-table :data="backups" class="panel" stripe>
          <el-table-column prop="name" label="名称" width="260" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="remark" label="备注" />
          <el-table-column prop="createdAt" label="时间" width="180" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="restoreBackup(row)">恢复</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogs.user" :title="forms.user.id ? '编辑用户' : '新增用户'" width="560px">
      <el-form :model="forms.user" label-width="86px">
        <el-form-item label="账号"><el-input v-model="forms.user.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="forms.user.password" placeholder="新增为初始密码，编辑留空不改" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="forms.user.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="forms.user.role">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级"><el-select v-model="forms.user.classId" clearable><el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="电话"><el-input v-model="forms.user.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="forms.user.email" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="forms.user.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogs.user=false">取消</el-button><el-button type="primary" @click="save('/admin/users', forms.user, 'user')">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="dialogs.classInfo" :title="forms.classInfo.id ? '编辑班级' : '新增班级'" width="520px">
      <el-form :model="forms.classInfo" label-width="80px">
        <el-form-item label="班级"><el-input v-model="forms.classInfo.name" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="forms.classInfo.grade" /></el-form-item>
        <el-form-item label="专业"><el-input v-model="forms.classInfo.major" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogs.classInfo=false">取消</el-button><el-button type="primary" @click="save('/admin/classes', forms.classInfo, 'classInfo')">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="dialogs.course" :title="forms.course.id ? '编辑课程' : '新增课程'" width="560px">
      <el-form :model="forms.course" label-width="80px">
        <el-form-item label="编号"><el-input v-model="forms.course.code" /></el-form-item>
        <el-form-item label="课程"><el-input v-model="forms.course.name" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="forms.course.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogs.course=false">取消</el-button><el-button type="primary" @click="save('/admin/courses', forms.course, 'course')">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="dialogs.teacherCourse" :title="forms.teacherCourse.id ? '编辑授课安排' : '新增授课安排'" width="560px">
      <el-form :model="forms.teacherCourse" label-width="80px">
        <el-form-item label="教师"><el-select v-model="forms.teacherCourse.teacherId"><el-option v-for="u in teachers" :key="u.id" :label="u.realName" :value="u.id" /></el-select></el-form-item>
        <el-form-item label="课程"><el-select v-model="forms.teacherCourse.courseId"><el-option v-for="c in courses" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="班级"><el-select v-model="forms.teacherCourse.classId"><el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogs.teacherCourse=false">取消</el-button><el-button type="primary" @click="save('/admin/teacher-courses', forms.teacherCourse, 'teacherCourse')">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="dialogs.announcement" :title="forms.announcement.id ? '编辑公告' : '发布公告'" width="620px">
      <el-form :model="forms.announcement" label-width="80px">
        <el-form-item label="标题"><el-input v-model="forms.announcement.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="forms.announcement.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="显示"><el-switch v-model="forms.announcement.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogs.announcement=false">取消</el-button><el-button type="primary" @click="save('/admin/announcements', forms.announcement, 'announcement')">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { createPager, exportXls, usePagedRows } from '../utils/tableTools'

const route = useRoute()
const active = ref(route.params.section || 'users')
const keyword = ref('')
const roleFilter = ref('ALL')
const roleFilterOptions = [
  { label: '全部', value: 'ALL' },
  { label: '管理员', value: 'ADMIN' },
  { label: '教师', value: 'TEACHER' },
  { label: '学生', value: 'STUDENT' }
]

const users = ref([])
const courses = ref([])
const classes = ref([])
const teacherCourses = ref([])
const announcements = ref([])
const permissions = ref([])
const logs = ref([])
const backups = ref([])
const checkedPermissions = ref([])
const permissionRole = ref('TEACHER')
const pagers = {
  users: createPager(),
  classes: createPager(),
  courses: createPager(),
  teacherCourses: createPager(),
  announcements: createPager(),
  logs: createPager(),
  backup: createPager()
}
const dialogs = reactive({ user: false, classInfo: false, course: false, teacherCourse: false, announcement: false })
const forms = reactive({
  user: {},
  classInfo: {},
  course: {},
  teacherCourse: {},
  announcement: {}
})

const teachers = computed(() => users.value.filter((u) => u.role === 'TEACHER'))
const q = computed(() => keyword.value.trim().toLowerCase())
const filteredUsers = computed(() => users.value.filter((u) => (roleFilter.value === 'ALL' || u.role === roleFilter.value) && match([u.username, u.realName, u.phone, u.email])))
const filteredClasses = computed(() => classes.value.filter((c) => match([c.name, c.grade, c.major])))
const filteredCourses = computed(() => courses.value.filter((c) => match([c.name, c.code, c.description])))
const filteredAnnouncements = computed(() => announcements.value.filter((a) => match([a.title, a.content])))
const teacherCourseRows = computed(() => teacherCourses.value)
const pagedUsers = usePagedRows(filteredUsers, pagers.users)
const pagedClasses = usePagedRows(filteredClasses, pagers.classes)
const pagedCourses = usePagedRows(filteredCourses, pagers.courses)
const pagedTeacherCourses = usePagedRows(teacherCourseRows, pagers.teacherCourses)
const pagedAnnouncements = usePagedRows(filteredAnnouncements, pagers.announcements)
const permissionGroups = computed(() => {
  const groups = new Map()
  permissions.value.forEach((p) => {
    if (!groups.has(p.module)) groups.set(p.module, [])
    groups.get(p.module).push(p)
  })
  return [...groups.entries()].map(([module, items]) => ({ module, items }))
})

function match(values) {
  return !q.value || values.some((v) => String(v || '').toLowerCase().includes(q.value))
}

async function load() {
  const [u, c, cls, tc, a, p, l, b] = await Promise.all([
    http.get('/admin/users'),
    http.get('/admin/courses'),
    http.get('/admin/classes'),
    http.get('/admin/teacher-courses'),
    http.get('/admin/announcements'),
    http.get('/admin/permissions'),
    http.get('/admin/logs'),
    http.get('/admin/backup-records')
  ])
  users.value = u
  courses.value = c
  classes.value = cls
  teacherCourses.value = tc
  announcements.value = a
  permissions.value = p
  logs.value = l
  backups.value = b
  await loadRolePermissions()
}

async function loadRolePermissions() {
  checkedPermissions.value = await http.get(`/admin/role-permissions/${permissionRole.value}`)
}

async function savePermissions() {
  await http.post(`/admin/role-permissions/${permissionRole.value}`, checkedPermissions.value)
  ElMessage.success('权限已保存，相关用户重新登录后生效')
}

async function loadLogs() {
  logs.value = await http.get('/admin/logs')
}

async function loadBackups() {
  backups.value = await http.get('/admin/backup-records')
}

async function createBackup() {
  const remark = await ElMessageBox.prompt('请输入备份备注', '创建备份', { confirmButtonText: '创建', cancelButtonText: '取消' })
  await http.post('/admin/backup-records', { remark: remark.value })
  ElMessage.success('备份记录已创建')
  loadBackups()
}

async function restoreBackup(row) {
  await ElMessageBox.confirm(`确认恢复 ${row.name} ?`, '恢复确认', { type: 'warning' })
  await http.post(`/admin/backup-records/${row.id}/restore`)
  ElMessage.success('恢复记录已更新')
  loadBackups()
}

function openUser(row = null) {
  forms.user = row ? { ...row, password: '' } : { role: 'STUDENT', enabled: 1, password: '123456' }
  dialogs.user = true
}

function openClass(row = null) {
  forms.classInfo = row ? { ...row } : { grade: '2023' }
  dialogs.classInfo = true
}

function openCourse(row = null) {
  forms.course = row ? { ...row } : {}
  dialogs.course = true
}

function openTeacherCourse(row = null) {
  forms.teacherCourse = row ? { ...row } : {}
  dialogs.teacherCourse = true
}

function openAnnouncement(row = null) {
  forms.announcement = row ? { ...row } : { enabled: 1 }
  dialogs.announcement = true
}

async function save(url, payload, dialog) {
  await http.post(url, payload)
  dialogs[dialog] = false
  ElMessage.success('保存成功')
  load()
}

async function remove(prefix, id) {
  await ElMessageBox.confirm('确认删除这条数据吗？', '删除确认', { type: 'warning' })
  await http.delete(prefix + id)
  ElMessage.success('删除成功')
  load()
}

function roleName(role) {
  return { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[role] || role
}

function userName(id) {
  return users.value.find((u) => u.id === id)?.realName || `用户${id}`
}

function courseName(id) {
  return courses.value.find((c) => c.id === id)?.name || `课程${id}`
}

function className(id) {
  return classes.value.find((c) => c.id === id)?.name || `班级${id}`
}

function exportUsers() {
  exportXls('用户管理', filteredUsers.value, [
    { label: '账号', prop: 'username' },
    { label: '姓名', prop: 'realName' },
    { label: '角色', formatter: (row) => roleName(row.role) },
    { label: '电话', prop: 'phone' },
    { label: '邮箱', prop: 'email' },
    { label: '状态', formatter: (row) => row.enabled ? '启用' : '停用' }
  ])
}

function exportClasses() {
  exportXls('班级管理', filteredClasses.value, [
    { label: '班级', prop: 'name' },
    { label: '年级', prop: 'grade' },
    { label: '专业', prop: 'major' }
  ])
}

function exportCourses() {
  exportXls('课程管理', filteredCourses.value, [
    { label: '编号', prop: 'code' },
    { label: '课程', prop: 'name' },
    { label: '简介', prop: 'description' }
  ])
}

function exportTeacherCourses() {
  exportXls('授课安排', teacherCourses.value, [
    { label: '教师', formatter: (row) => userName(row.teacherId) },
    { label: '课程', formatter: (row) => courseName(row.courseId) },
    { label: '班级', formatter: (row) => className(row.classId) }
  ])
}

function exportAnnouncements() {
  exportXls('公告管理', filteredAnnouncements.value, [
    { label: '标题', prop: 'title' },
    { label: '内容', prop: 'content' },
    { label: '状态', formatter: (row) => row.enabled ? '显示' : '隐藏' }
  ])
}

onMounted(load)
watch(() => route.params.section, (section) => {
  active.value = section || 'users'
})
</script>

<style scoped>
.route-tabs :deep(.el-tabs__header) {
  display: none;
}

.search {
  max-width: 320px;
}

.el-select {
  width: 100%;
}

.permission-grid {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 14px;
}

.role-list,
.permission-list {
  display: grid;
  gap: 12px;
}

.permission-group {
  border-top: 1px solid var(--line);
  padding-top: 12px;
}

.permission-items {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-top: 8px;
}
</style>
