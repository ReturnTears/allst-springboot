<template>
  <section class="grid">
    <article class="panel">
      <h2>当前身份</h2>
      <p>{{ me.username }} / {{ me.displayName }}</p>
      <p>权限：{{ (me.permissions || []).join('、') || '无' }}</p>
      <p v-if="me.passwordExpired" class="error">密码已过期，请先到口令页修改。</p>
      <p v-else class="ok">密码仍在有效期内。</p>
    </article>
    <article class="panel">
      <h2>公开探测</h2>
      <button @click="probe">调用 /hello/security</button>
      <p class="mono">{{ hello }}</p>
    </article>
  </section>
</template>

<script setup>
import { ref } from 'vue'

defineProps({ me: { type: Object, required: true } })
const hello = ref('')

async function probe() {
  const response = await fetch('/hello/security')
  hello.value = await response.text()
}
</script>
