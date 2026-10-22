<template>
  <section class="login-page">
    <div class="login-copy">
      <p class="eyebrow">Allst Security</p>
      <h1>安全规范控制台</h1>
      <p>登录后可以查看身份、用户、口令、会话、审计和已装载的扩展点。接口说明在项目 docs 目录。</p>
      <ul class="accounts">
        <li>admin / Manage@123</li>
        <li>alice / Reader@123</li>
        <li>auditor / Audit@123</li>
      </ul>
    </div>
    <form class="login-card" @submit.prevent="submit">
      <label>用户名<input v-model.trim="username" autocomplete="username" /></label>
      <label>口令<input v-model="password" type="password" autocomplete="current-password" /></label>
      <button type="submit" :disabled="loading">登录</button>
      <p class="error">{{ error }}</p>
    </form>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, applyTokens } from '../api'

const router = useRouter()
const username = ref('alice')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const body = await api('/api/auth/login', {
      method: 'POST',
      auth: false,
      json: { username: username.value, password: password.value }
    })
    applyTokens(body.data)
    await router.push('/app/overview')
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}
</script>
