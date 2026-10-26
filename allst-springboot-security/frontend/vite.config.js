import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 开发时浏览器只访问 5173，由 Vite 转到 1235。这样会话 Cookie 和页面同源，浏览器才会带上它。
      '/api': 'http://localhost:1235',
      '/session': 'http://localhost:1235',
      '/basic': 'http://localhost:1235',
      '/hello': 'http://localhost:1235'
    }
  }
})
