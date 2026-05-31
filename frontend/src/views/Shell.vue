<template>
  <el-container class="shell">
    <el-aside width="232px">
      <div class="brand">
        <strong>智能考试系统</strong>
        <span>{{ auth.user?.realName }} · {{ roleName }}</span>
      </div>
      <el-menu router :default-active="$route.path" :default-openeds="openMenus" background-color="#263445" text-color="#cbd5e1" active-text-color="#ffffff">
        <el-menu-item v-if="has('dashboard')" index="/">
          <el-icon><House /></el-icon>
          <span>工作台</span>
        </el-menu-item>
        <el-sub-menu v-for="group in menuGroups" :key="group.name" :index="group.name">
          <template #title>
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.name }}</span>
          </template>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.name }}</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <span>基于 SpringBoot + Vue 的校园智能在线考试系统</span>
        <el-button @click="logout">退出</el-button>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import {
  Bell,
  Collection,
  DataAnalysis,
  Document,
  EditPen,
  House,
  Key,
  Medal,
  Notebook,
  Reading,
  School,
  Tickets,
  TrendCharts,
  User,
  UserFilled
} from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const roleName = computed(() => ({ ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[auth.user?.role] || '用户'))
const moduleIcons = {
  系统管理: School,
  教学管理: Reading,
  学生中心: UserFilled
}
const menuDefinitions = [
  { code: 'admin:user', name: '用户管理', module: '系统管理', path: '/admin/users', icon: User },
  { code: 'admin:class', name: '班级管理', module: '系统管理', path: '/admin/classes', icon: Collection },
  { code: 'admin:course', name: '课程管理', module: '系统管理', path: '/admin/courses', icon: Notebook },
  { code: 'admin:teaching', name: '授课安排', module: '系统管理', path: '/admin/teacher-courses', icon: Reading },
  { code: 'admin:announcement', name: '公告管理', module: '系统管理', path: '/admin/announcements', icon: Bell },
  { code: 'admin:permission', name: '权限管理', module: '系统管理', path: '/admin/permissions', icon: Key },
  { code: 'admin:logs', name: '操作日志', module: '系统管理', path: '/admin/logs', icon: DataAnalysis },
  { code: 'admin:backup', name: '备份恢复', module: '系统管理', path: '/admin/backup', icon: Notebook },
  { code: 'teacher:questions', name: '题库管理', module: '教学管理', path: '/teacher/questions', icon: EditPen },
  { code: 'teacher:papers', name: '试卷管理', module: '教学管理', path: '/teacher/papers', icon: Tickets },
  { code: 'teacher:scores', name: '成绩管理', module: '教学管理', path: '/teacher/scores', icon: TrendCharts },
  { code: 'teacher:manual', name: '人工阅卷', module: '教学管理', path: '/teacher/manual', icon: Notebook },
  { code: 'teacher:analysis', name: '成绩分析', module: '教学管理', path: '/teacher/analysis', icon: DataAnalysis },
  { code: 'teacher:monitor', name: '考试监控', module: '教学管理', path: '/teacher/monitor', icon: Bell },
  { code: 'teacher:appeals', name: '成绩复查', module: '教学管理', path: '/teacher/appeals', icon: Medal },
  { code: 'student:exams', name: '在线考试', module: '学生中心', path: '/student/exams', icon: Document },
  { code: 'student:attempts', name: '历史成绩', module: '学生中心', path: '/student/attempts', icon: DataAnalysis },
  { code: 'student:wrong', name: '错题解析', module: '学生中心', path: '/student/wrong', icon: Medal },
  { code: 'student:advice', name: '学习建议', module: '学生中心', path: '/student/advice', icon: Notebook },
  { code: 'student:appeals', name: '成绩申诉', module: '学生中心', path: '/student/appeals', icon: Key },
  { code: 'student:notifications', name: '通知中心', module: '学生中心', path: '/student/notifications', icon: Bell },
  { code: 'student:profile', name: '个人中心', module: '学生中心', path: '/student/profile', icon: UserFilled }
]

const permissionSet = computed(() => new Set(auth.permissions || []))
const menuGroups = computed(() => {
  const groups = new Map()
  menuDefinitions.filter((item) => has(item.code)).forEach((item) => {
    if (!groups.has(item.module)) groups.set(item.module, [])
    groups.get(item.module).push(item)
  })
  return [...groups.entries()].map(([name, items]) => ({ name, items, icon: moduleIcons[name] || Collection }))
})
const openMenus = computed(() => menuGroups.value.map((group) => group.name))

function has(code) {
  return permissionSet.value.has(code)
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.shell {
  height: 100vh;
  overflow: hidden;
}

.el-aside {
  height: 100vh;
  overflow-y: auto;
  background:
    linear-gradient(180deg, rgba(0, 161, 214, .16), transparent 28%),
    var(--nav);
}

.brand {
  height: 86px;
  padding: 18px;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,.08);
}

.brand strong {
  display: block;
  font-size: 18px;
}

.brand span {
  display: block;
  margin-top: 6px;
  color: #aebecd;
  font-size: 13px;
}

.el-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  background: rgba(255, 255, 255, .86);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--line);
  font-weight: 700;
}

.el-main {
  padding: 0;
  height: calc(100vh - 64px);
  overflow-y: auto;
}
</style>
