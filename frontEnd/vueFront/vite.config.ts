import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  server: {
    // 开发时将 /auth-service, /admin-service 等转发到后端网关，避免跨域问题
    proxy: {
      '/auth-service': {
        target: 'http://localhost:8002',
        changeOrigin: true,
        secure: false,
      },
      '/doctor-service': {
        target: 'http://localhost:8003',
        changeOrigin: true,
      },
      '/patient-service': {
        target: 'http://localhost:8004',
        changeOrigin: true,
      },
      '/admin-service': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
      '/files/avatar': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
      '/ai-service': {
        target: 'http://localhost:8001',
        changeOrigin: true,
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('API代理错误:', err);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('代理到AI服务:', req.method, req.url);
          });
        }
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})