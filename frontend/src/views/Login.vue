<template>
  <main class="login">
    <div class="login-bg" aria-hidden="true">
      <span class="shape shape-a"></span>
      <span class="shape shape-b"></span>
      <span class="shape shape-c"></span>
      <span class="shape shape-d"></span>
    </div>
    <section class="login-card">
      <div>
        <p class="eyebrow">Campus Exam AI</p>
        <h1>校园智能在线考试系统</h1>
        <p class="muted">在线考试、自动阅卷、错题 AI 解析一体化演示平台</p>
      </div>
      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="form.username" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="submit">登录</el-button>
      </el-form>
      <div class="quick">
        <el-button plain @click="fill('admin')">管理员</el-button>
        <el-button plain @click="fill('teacher')">教师</el-button>
        <el-button plain @click="fill('student')">学生</el-button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

function fill(username) {
  form.username = username
  form.password = '123456'
}

async function submit() {
  loading.value = true
  try {
    await auth.login(form)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 24% 20%, rgba(0, 161, 214, .18), transparent 26%),
    radial-gradient(circle at 78% 78%, rgba(251, 114, 153, .14), transparent 28%),
    linear-gradient(135deg, #f8fbff 0%, #edf7ff 48%, #fff7fb 100%),
    #f5f7fb;
}

.login::before,
.login::after {
  content: "";
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
}

.login::before {
  width: 620px;
  height: 620px;
  left: -240px;
  bottom: -260px;
  background: rgba(0, 161, 214, .08);
}

.login::after {
  width: 460px;
  height: 460px;
  right: -180px;
  top: -180px;
  background: rgba(251, 114, 153, .10);
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
  display: block;
  border: 1px solid rgba(255, 255, 255, .75);
  background: rgba(255, 255, 255, .36);
  box-shadow: 0 26px 70px rgba(31, 42, 68, .08);
  backdrop-filter: blur(10px);
}

.shape-a {
  width: 240px;
  height: 150px;
  left: 9%;
  top: 18%;
  border-radius: 28px;
  transform: rotate(-9deg);
}

.shape-b {
  width: 170px;
  height: 170px;
  right: 12%;
  top: 24%;
  border-radius: 50%;
}

.shape-c {
  width: 280px;
  height: 120px;
  left: 13%;
  bottom: 16%;
  border-radius: 999px;
  transform: rotate(8deg);
}

.shape-d {
  width: 130px;
  height: 130px;
  right: 22%;
  bottom: 14%;
  border-radius: 30px;
  transform: rotate(14deg);
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(460px, 100%);
  background: rgba(255, 255, 255, .92);
  border: 1px solid rgba(217, 226, 236, .84);
  border-radius: 14px;
  padding: 32px;
  box-shadow: 0 28px 80px rgba(31, 42, 55, .16);
  backdrop-filter: blur(18px);
}

.eyebrow {
  margin: 0 0 8px;
  color: #0f766e;
  font-weight: 800;
  letter-spacing: 0;
}

h1 {
  margin: 0 0 8px;
  font-size: 30px;
}

.el-button--large {
  width: 100%;
}

.quick {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 14px;
}

@media (max-width: 900px) {
  .shape {
    display: none;
  }
}
</style>
