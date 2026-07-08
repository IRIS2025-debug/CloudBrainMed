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
  build: {
    rolldownOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) return null
          if (id.includes('/vue/') || id.includes('/vue-router/') || id.includes('/pinia/')) {
            return 'vue-vendor'
          }
          if (id.includes('/@vueuse/')) {
            return 'vueuse-vendor'
          }
          if (
            id.includes('/@floating-ui/') ||
            id.includes('/@popperjs/') ||
            id.includes('/@sxzz/')
          ) {
            return 'floating-vendor'
          }
          if (
            id.includes('/async-validator/') ||
            id.includes('/dayjs/') ||
            id.includes('/lodash') ||
            id.includes('/lodash-unified/') ||
            id.includes('/memoize-one/') ||
            id.includes('/normalize-wheel-es/') ||
            id.includes('/@ctrl/tinycolor/')
          ) {
            return 'element-utils-vendor'
          }
          if (id.includes('/@element-plus/icons-vue/')) {
            return 'element-icons-vendor'
          }
          if (
            id.includes('/element-plus/es/components/table') ||
            id.includes('/element-plus/es/components/table-column') ||
            id.includes('/element-plus/es/components/pagination')
          ) {
            return 'element-table-vendor'
          }
          if (
            id.includes('/element-plus/es/components/form') ||
            id.includes('/element-plus/es/components/form-item')
          ) {
            return 'element-form-vendor'
          }
          if (
            id.includes('/element-plus/es/components/input') ||
            id.includes('/element-plus/es/components/input-number')
          ) {
            return 'element-input-vendor'
          }
          if (
            id.includes('/element-plus/es/components/select') ||
            id.includes('/element-plus/es/components/option')
          ) {
            return 'element-select-vendor'
          }
          if (
            id.includes('/element-plus/es/components/date-picker') ||
            id.includes('/element-plus/es/components/time-picker')
          ) {
            return 'element-picker-vendor'
          }
          if (
            id.includes('/element-plus/es/components/checkbox') ||
            id.includes('/element-plus/es/components/checkbox-group') ||
            id.includes('/element-plus/es/components/radio') ||
            id.includes('/element-plus/es/components/slider')
          ) {
            return 'element-choice-vendor'
          }
          if (id.includes('/element-plus/es/components/upload')) {
            return 'element-upload-vendor'
          }
          if (
            id.includes('/element-plus/es/components/dialog') ||
            id.includes('/element-plus/es/components/dropdown') ||
            id.includes('/element-plus/es/components/dropdown-item') ||
            id.includes('/element-plus/es/components/dropdown-menu') ||
            id.includes('/element-plus/es/components/popconfirm') ||
            id.includes('/element-plus/es/components/message') ||
            id.includes('/element-plus/es/components/message-box') ||
            id.includes('/element-plus/es/components/popper') ||
            id.includes('/element-plus/es/components/tooltip')
          ) {
            return 'element-overlay-vendor'
          }
          if (id.includes('/element-plus/')) {
            return 'element-vendor'
          }
          if (id.includes('/axios/')) {
            return 'network-vendor'
          }
          return 'vendor'
        },
      },
      onLog(level, log, defaultHandler) {
        if (
          level === 'warn' &&
          log.code === 'INVALID_ANNOTATION' &&
          log.message.includes('@vueuse/core')
        ) {
          return
        }
        defaultHandler(level, log)
      },
    },
  },
  server: {
    // 开发时将 /auth-service, /admin-service 等转发到后端网关，避免跨域问题
    proxy: {
      '/exam-service': {
        target: 'http://localhost:8006',
        changeOrigin: true,
        secure: false,
      },
      '/auth-service': {
        target: 'http://localhost:8002',
        changeOrigin: true,
        secure: false,
      },
      '/doctor-service': {
        target: 'http://localhost:8003',
        changeOrigin: true,
      },
      '/inspection-doctor': {
        target: 'http://localhost:8003',
        changeOrigin: true,
      },
      '/patient-service': {
        target: 'http://localhost:8004',
        changeOrigin: true,
      },
      '/admin-service/ml': {
        target: 'http://localhost:8001',
        changeOrigin: true,
      },
      '/admin-service': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
      '/files/avatar/doctor': {
        target: 'http://localhost:8003',
        changeOrigin: true,
      },
      '/files/avatar/admin': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
      '/files/avatar/patient': {
        target: 'http://localhost:8004',
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
      },
      '/api/inspection-doctor': {
        target: 'http://localhost:8003',
        changeOrigin: true,
      },
      '/payment-service': {
        target: 'http://localhost:8005',
        changeOrigin: true,
      },
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
