<template>
  <article class="panel">
    <h2>已装载的扩展点</h2>
    <h3>协议</h3>
    <table v-if="data.protocols">
      <thead><tr><th>名称</th><th>状态</th><th>说明</th></tr></thead>
      <tbody>
        <tr v-for="item in data.protocols" :key="item.id">
          <td>{{ item.title }}</td>
          <td>{{ item.enabled ? '启用' : '关闭' }}</td>
          <td>{{ item.description }}</td>
        </tr>
      </tbody>
    </table>
    <p>登录守卫：{{ (data.loginGuards || []).join('、') }}</p>
    <p>令牌扩展：{{ (data.tokenCustomizers || []).join('、') }}</p>
    <p>过滤链：{{ (data.filterChains || []).join('、') }}</p>
    <p>方法安全：{{ data.methodSecurity }}</p>
    <p class="error">{{ error }}</p>
  </article>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api'

const data = ref({})
const error = ref('')

onMounted(async () => {
  try {
    const body = await api('/api/security/modules')
    data.value = body.data
  } catch (err) {
    error.value = err.message
  }
})
</script>
