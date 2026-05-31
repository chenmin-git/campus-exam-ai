import { defineStore } from 'pinia'
import http from '../api/http'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]')
  }),
  actions: {
    async login(form) {
      const data = await http.post('/auth/login', form)
      this.token = data.token
      this.user = data.user
      this.permissions = data.permissions || []
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
    },
    logout() {
      this.token = ''
      this.user = null
      this.permissions = []
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('permissions')
    }
  }
})
