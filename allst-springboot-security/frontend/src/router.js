import { createRouter, createWebHistory } from 'vue-router'
import LoginView from './views/LoginView.vue'
import ShellView from './views/ShellView.vue'
import { restoreSession } from './api'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'login', component: LoginView },
    { path: '/app/:panel?', name: 'app', component: ShellView }
  ]
})

router.beforeEach(async (to) => {
  if (to.name !== 'app') {
    return true
  }
  const ok = await restoreSession()
  if (!ok) {
    return { name: 'login' }
  }
  return true
})
