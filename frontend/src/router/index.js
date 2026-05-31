import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import Login from '../views/Login.vue'
import Shell from '../views/Shell.vue'
import Dashboard from '../views/Dashboard.vue'
import AdminView from '../views/AdminView.vue'
import TeacherView from '../views/TeacherView.vue'
import StudentView from '../views/StudentView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: Login },
    {
      path: '/',
      component: Shell,
      children: [
        { path: '', component: Dashboard },
        { path: 'admin', redirect: '/admin/users' },
        { path: 'admin/:section', component: AdminView },
        { path: 'teacher', redirect: '/teacher/questions' },
        { path: 'teacher/:section', component: TeacherView },
        { path: 'student', redirect: '/student/exams' },
        { path: 'student/:section', component: StudentView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.token) return '/login'
  if (to.path === '/login' && auth.token) return '/'
  if (to.path === '/admin') return '/admin/users'
  if (to.path === '/teacher') return '/teacher/questions'
  if (to.path === '/student') return '/student/exams'
  const routePermissions = {
    '/admin/users': 'admin:user',
    '/admin/classes': 'admin:class',
    '/admin/courses': 'admin:course',
    '/admin/teacher-courses': 'admin:teaching',
    '/admin/announcements': 'admin:announcement',
    '/admin/permissions': 'admin:permission',
    '/admin/logs': 'admin:logs',
    '/admin/backup': 'admin:backup',
    '/teacher/questions': 'teacher:questions',
    '/teacher/papers': 'teacher:papers',
    '/teacher/scores': 'teacher:scores',
    '/teacher/manual': 'teacher:manual',
    '/teacher/analysis': 'teacher:analysis',
    '/teacher/monitor': 'teacher:monitor',
    '/teacher/appeals': 'teacher:appeals',
    '/student/exams': 'student:exams',
    '/student/attempts': 'student:attempts',
    '/student/wrong': 'student:wrong',
    '/student/advice': 'student:advice',
    '/student/appeals': 'student:appeals',
    '/student/profile': 'student:profile'
  }
  const code = routePermissions[to.path]
  if (code && !auth.permissions.includes(code)) return '/'
})

export default router
