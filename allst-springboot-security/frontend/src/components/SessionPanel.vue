<template>
  <section class="grid">
    <article class="panel stack">
      <h2>会话登录</h2>
      <p>这条链使用 Cookie 和 CSRF，和左侧 JWT 登录互相独立。</p>
      <label>用户名<input v-model.trim="username" /></label>
      <label>口令<input v-model="password" type="password" /></label>
      <div>
        <button @click="login">建立会话</button>
        <button class="ghost" @click="logout">退出会话</button>
      </div>
      <p class="mono">{{ sessionUser }}</p>
      <p class="error">{{ error }}</p>
    </article>
    <article class="panel">
      <h2>在线会话</h2>
      <button v-if="admin" @click="loadActive">刷新</button>
      <p v-else>需要管理员查看在线会话。</p>
      <table v-if="sessions.length">
        <thead><tr><th>用户</th><th>会话前缀</th><th>最近请求</th></tr></thead>
        <tbody>
          <tr v-for="item in sessions" :key="item.sessionIdPrefix + item.username">
            <td>{{ item.username }}</td>
            <td class="mono">{{ item.sessionIdPrefix }}</td>
            <td>{{ item.lastRequest }}</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { sessionApi } from '../api'

defineProps({ admin: { type: Boolean, default: false } })
const username = ref('alice')
const password = ref('')
const sessionUser = ref('尚未建立会话')
const sessions = ref([])
const error = ref('')

async function login() {
  error.value = ''
  try {
    await sessionApi('/session/csrf')
    const body = new URLSearchParams()
    body.set('username', username.value)
    body.set('password', password.value)
    const result = await sessionApi('/session/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body
    })
    sessionUser.value = result.data.username + ' / ' + result.data.roles.join('、')
  } catch (err) {
    error.value = err.message
  }
}

async function logout() {
  error.value = ''
  try {
    await sessionApi('/session/logout', { method: 'POST' })
    sessionUser.value = '会话已退出'
    sessions.value = []
  } catch (err) {
    error.value = err.message
  }
}

async function loadActive() {
  error.value = ''
  try {
    const body = await sessionApi('/session/active')
    sessions.value = body.data
  } catch (err) {
    error.value = err.message
  }
}
</script>
