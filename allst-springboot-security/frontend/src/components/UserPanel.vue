<template>
  <section class="grid">
    <article class="panel">
      <h2>用户</h2>
      <table>
        <thead>
          <tr><th>用户名</th><th>显示名</th><th>角色</th><th>状态</th></tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.username }}</td>
            <td>{{ user.displayName }}</td>
            <td>{{ user.roles.join('、') }}</td>
            <td>{{ user.enabled ? '启用' : '停用' }}</td>
          </tr>
        </tbody>
      </table>
    </article>
    <article v-if="canWrite" class="panel stack">
      <h2>新建</h2>
      <label>用户名<input v-model.trim="form.username" /></label>
      <label>显示名<input v-model.trim="form.displayName" /></label>
      <label>口令<input v-model="form.password" type="password" /></label>
      <label>角色
        <select v-model="form.roleCodes[0]">
          <option>ROLE_USER</option>
          <option>ROLE_AUDITOR</option>
          <option>ROLE_ADMIN</option>
        </select>
      </label>
      <button @click="create">创建</button>
      <p class="error">{{ error }}</p>
    </article>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '../api'

const props = defineProps({ me: { type: Object, required: true } })
const users = ref([])
const error = ref('')
const form = reactive({ username: '', displayName: '', password: '', roleCodes: ['ROLE_USER'] })
const canWrite = computed(() => (props.me.permissions || []).includes('user:write'))

async function load() {
  const body = await api('/api/users')
  users.value = body.data
}

async function create() {
  error.value = ''
  try {
    await api('/api/users', { method: 'POST', json: form })
    form.username = ''
    form.displayName = ''
    form.password = ''
    await load()
  } catch (err) {
    error.value = err.message
  }
}

onMounted(load)
</script>
