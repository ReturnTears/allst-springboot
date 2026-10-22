import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:1235',
      '/session': 'http://localhost:1235',
      '/basic': 'http://localhost:1235',
      '/hello': 'http://localhost:1235'
    }
  }
})
