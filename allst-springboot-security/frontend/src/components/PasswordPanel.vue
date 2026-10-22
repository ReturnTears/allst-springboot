<template>
  <section class="panel stack" style="max-width: 32rem;">
    <h2>修改口令</h2>
    <p v-if="policy">{{ policy.rule }}。历史 {{ policy.historySize }} 条，有效期 {{ policy.maxAgeDays }} 天。</p>
    <label>原口令<input v-model="oldPassword" type="password" /></label>
    <label>新口令<input v-model="newPassword" type="password" /></label>
    <button @click="change">更新</button>
    <p class="ok">{{ message }}</p>
    <p class="error">{{ error }}</p>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api, applyTokens } from '../api'

const oldPassword = ref('')
const newPassword = ref('')
const policy = ref(null)
const message = ref('')
const error = ref('')

onMounted(async () => {
  const body = await api('/api/password/policy', { auth: false })
  policy.value = body.data
})

async function change() {
  message.value = ''
  error.value = ''
  try {
    const body = await api('/api/password/change', {
      method: 'POST',
      json: { oldPassword: oldPassword.value, newPassword: newPassword.value }
    })
    applyTokens(body.data)
    oldPassword.value = ''
    newPassword.value = ''
    message.value = '口令已更新，当前令牌已更换。'
  } catch (err) {
    error.value = err.message
  }
}
</script>
