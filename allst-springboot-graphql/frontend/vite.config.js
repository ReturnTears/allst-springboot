import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/graphql': {
        target: 'http://127.0.0.1:8088',
        changeOrigin: true
      },
      '/subscriptions': {
        target: 'http://127.0.0.1:8088',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
