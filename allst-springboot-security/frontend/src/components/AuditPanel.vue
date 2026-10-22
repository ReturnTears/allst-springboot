<template>
  <article class="panel">
    <h2>登录审计</h2>
    <table>
      <thead>
        <tr><th>时间</th><th>用户</th><th>结果</th><th>地址</th><th>说明</th></tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id">
          <td>{{ row.createdAt }}</td>
          <td>{{ row.username }}</td>
          <td>{{ row.success ? '成功' : '失败' }}</td>
          <td>{{ row.ip }}</td>
          <td>{{ row.message }}</td>
        </tr>
      </tbody>
    </table>
    <p class="error">{{ error }}</p>
  </article>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const rows = ref([])
const error = ref('')

onMounted(async () => {
  try {
    const body = await api('/api/audit/logins')
    rows.value = body.data
  } catch (err) {
    error.value = err.message
  }
})
</script>
