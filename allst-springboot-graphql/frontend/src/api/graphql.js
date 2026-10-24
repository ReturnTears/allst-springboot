export async function gql(query, variables) {
  const response = await fetch(import.meta.env.VITE_GRAPHQL_HTTP || '/graphql', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, variables })
  })
  const payload = await response.json()
  if (payload.errors && payload.errors.length) {
    const first = payload.errors[0]
    const code = first.extensions && first.extensions.code
    const error = new Error(first.message || '请求失败')
    error.code = code
    throw error
  }
  return payload.data
}

export function subscribeMemberships(onEvent, onStatus) {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const url = import.meta.env.VITE_GRAPHQL_WS || `${protocol}//${location.host}/subscriptions`
  const query = `subscription {
    membershipChanged {
      id
      status
      plan
      reader { username }
      book { name }
    }
  }`
  let socket
  let stopped = false
  let attempt = 0

  function connect() {
    socket = new WebSocket(url, 'graphql-ws')
    socket.onopen = () => {
      socket.send(JSON.stringify({ type: 'connection_init', payload: {} }))
    }
    socket.onmessage = (event) => {
      const message = JSON.parse(event.data)
      if (message.type === 'connection_ack') {
        attempt = 0
        onStatus('open')
        socket.send(JSON.stringify({ id: '1', type: 'start', payload: { query } }))
      } else if (message.type === 'data' && message.payload && message.payload.data) {
        onEvent(message.payload.data.membershipChanged)
      } else if (message.type === 'error' || message.type === 'connection_error') {
        onStatus('error')
      }
    }
    socket.onerror = () => onStatus('error')
    socket.onclose = () => {
      onStatus('closed')
      if (!stopped) {
        attempt += 1
        window.setTimeout(connect, Math.min(8000, 800 * attempt))
      }
    }
  }

  connect()
  return () => {
    stopped = true
    if (socket) {
      socket.close()
    }
  }
}
