import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers['X-Token'] = token
  return config
})

http.interceptors.response.use((response) => {
  const body = response.data
  if (body && body.success === false) {
    ElMessage.error(body.message || '请求失败')
    if ((body.message || '').includes('登录')) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (location.pathname !== '/login') {
        location.href = '/login'
      }
    }
    return Promise.reject(new Error(body.message || '请求失败'))
  }
  return body?.data ?? body
})

export default http
