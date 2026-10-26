// 访问令牌只放内存：刷新页面就没了，但页面脚本被注入时也不在 localStorage 里长期躺着。
// 刷新令牌放 sessionStorage，关掉标签页就消失。这是演示取舍，生产环境更稳妥的是把刷新令牌放 HttpOnly Cookie。
// 业务请求走 api()：带 Bearer，401 时先 POST /api/auth/refresh，再用新访问令牌重试一次。
// 会话请求走 sessionApi()：credentials 带上 Cookie，并把 XSRF-TOKEN 抄到请求头 X-XSRF-TOKEN。
const refreshKey = 'allst.refresh'
let accessToken = ''
let restored = false

export function hasAccessToken() {
  return Boolean(accessToken)
}

export function applyTokens(data) {
  accessToken = data.accessToken || ''
  if (data.refreshToken) {
    sessionStorage.setItem(refreshKey, data.refreshToken)
  }
}

export function clearTokens() {
  accessToken = ''
  sessionStorage.removeItem(refreshKey)
  restored = true
}

async function refreshTokens() {
  const refreshToken = sessionStorage.getItem(refreshKey)
  if (!refreshToken) {
    return false
  }
  const response = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok || body.code !== 0) {
    clearTokens()
    return false
  }
  applyTokens(body.data)
  return true
}

export async function restoreSession() {
  if (accessToken) {
    return true
  }
  if (restored && !sessionStorage.getItem(refreshKey)) {
    return false
  }
  restored = true
  return refreshTokens()
}

export async function api(path, options = {}) {
  const run = async () => {
    const headers = { ...(options.headers || {}) }
    if (options.json !== undefined) {
      headers['Content-Type'] = 'application/json'
    }
    if (options.auth !== false && accessToken) {
      headers.Authorization = 'Bearer ' + accessToken
    }
    const response = await fetch(path, {
      method: options.method || 'GET',
      headers,
      body: options.json !== undefined ? JSON.stringify(options.json) : options.body
    })
    const body = await response.json().catch(() => ({}))
    return { response, body }
  }

  let result = await run()
  if (result.response.status === 401 && options.auth !== false && options.retried !== true) {
    const ok = await refreshTokens()
    if (ok) {
      return api(path, { ...options, retried: true })
    }
  }
  if (!result.response.ok || (result.body && result.body.code && result.body.code !== 0)) {
    const error = new Error(result.body.message || '请求失败')
    error.status = result.response.status
    error.code = result.body.code
    throw error
  }
  return result.body
}

function readCookie(name) {
  const matched = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  return matched ? decodeURIComponent(matched[1]) : ''
}

export async function sessionApi(path, options = {}) {
  const headers = { ...(options.headers || {}) }
  const csrf = readCookie('XSRF-TOKEN')
  if (csrf) {
    headers['X-XSRF-TOKEN'] = csrf
  }
  const response = await fetch(path, {
    method: options.method || 'GET',
    headers,
    body: options.body,
    credentials: 'include'
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok || (body && body.code && body.code !== 0)) {
    const error = new Error(body.message || '请求失败')
    error.status = response.status
    throw error
  }
  return body
}
