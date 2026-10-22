<template>
  <div class="shell">
    <nav>
      <strong>规范控制台</strong>
      <router-link to="/app/overview">概览</router-link>
      <router-link v-if="can('user:read')" to="/app/users">用户</router-link>
      <router-link to="/app/password">口令</router-link>
      <router-link to="/app/session">会话</router-link>
      <router-link v-if="can('audit:read')" to="/app/audit">审计</router-link>
      <router-link to="/app/modules">扩展点</router-link>
    </nav>
    <main>
      <div class="top">
        <div>
          <strong>{{ me.displayName || me.username }}</strong>
          <div>{{ (me.roles || []).join('、') }}</div>
        </div>
        <button class="ghost" @click="logout">退出</button>
      </div>
      <OverviewPanel v-if="panel === 'overview'" :me="me" />
      <UserPanel v-else-if="panel === 'users'" :me="me" />
      <PasswordPanel v-else-if="panel === 'password'" />
      <SessionPanel v-else-if="panel === 'session'" :admin="can('session:read') || isAdmin" />
      <AuditPanel v-else-if="panel === 'audit'" />
      <ModulePanel v-else />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, clearTokens } from '../api'
import OverviewPanel from '../components/OverviewPanel.vue'
import UserPanel from '../components/UserPanel.vue'
import PasswordPanel from '../components/PasswordPanel.vue'
import SessionPanel from '../components/SessionPanel.vue'
import AuditPanel from '../components/AuditPanel.vue'
import ModulePanel from '../components/ModulePanel.vue'

const route = useRoute()
const router = useRouter()
const me = ref({ roles: [], permissions: [] })
const panel = computed(() => route.params.panel || 'overview')
const isAdmin = computed(() => (me.value.roles || []).includes('ROLE_ADMIN'))

function can(permission) {
  return (me.value.permissions || []).includes(permission)
}

onMounted(async () => {
  const body = await api('/api/auth/me')
  me.value = body.data
})

async function logout() {
  try {
    const refreshToken = sessionStorage.getItem('allst.refresh')
    await api('/api/auth/logout', { method: 'POST', json: { refreshToken } })
  } catch (err) {
    // 令牌已经失效时仍然清掉本地状态。
  }
  clearTokens()
  await router.push('/')
}
</script>
