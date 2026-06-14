<template>
  <div class="app-shell" v-if="showSidebar">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand" @click="$router.push('/')">
        <div class="brand-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="8" fill="#2563eb"/>
            <path d="M6 14h4l2-5 4 10 2-5h4" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <span class="brand-text" v-show="!collapsed">CloudBrain<span class="brand-accent">Med</span></span>
      </div>

      <nav class="nav">
        <!-- 不再硬编码，统一用 v-for 渲染 -->
        <template v-for="item in menuItems" :key="item.path">
          <div class="nav-group-label" v-show="!collapsed && showGroupLabel(item)">
            {{ item.group }}
          </div>
          <router-link :to="item.path" class="nav-item">
            <el-icon :size="20">
              <component :is="iconMap[item.icon]" />
            </el-icon>
            <span v-show="!collapsed">{{ item.title }}</span>
          </router-link>
        </template>
      </nav>

      <div class="sidebar-footer" v-show="!collapsed">
        <div class="footer-status">
          <div class="status-dot"></div>
          <span>系统运行中</span>
        </div>
        <el-button class="logout-btn" @click="handleLogout" text>
          <el-icon :size="16"><SwitchButton /></el-icon>
          <span>退出登录</span>
        </el-button>
      </div>
    </aside>

    <main class="main">
      <RouterView />
    </main>
  </div>
  <RouterView v-else />
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { computed, ref, watch } from 'vue'
import { ElIcon, ElMessage } from 'element-plus'
import {
  UserFilled,
  List,
  DataAnalysis,
  CollectionTag,
  Cpu,
  Camera,
  HomeFilled,
  SwitchButton,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

// 退出登录
const handleLogout = () => {
  sessionStorage.removeItem('token')
  sessionStorage.removeItem('roleType')
  sessionStorage.removeItem('userRole')
  ElMessage.success('已退出登录')
  router.push('/login')
}

const showSidebar = computed(() => route.path !== '/login')

// 图标映射（新增 HomeFilled）
const iconMap: Record<string, any> = {
  HomeFilled,    // 新增
  UserFilled,
  List,
  DataAnalysis,
  CollectionTag,
  Cpu,
  Camera,
}

// ==================== 菜单配置（包含首页概览）====================
const doctorMenus = [
  { path: '/', title: '首页概览', icon: 'HomeFilled', group: '医生端' },      // 新增
  { path: '/doctor/profile', title: '医生个人信息', icon: 'UserFilled', group: '医生端' },
  { path: '/doctor/consult', title: '接诊工作台', icon: 'List', group: '医生端' },
  { path: '/doctor/ai-medicine', title: 'AI 药物推荐', icon: 'DataAnalysis', group: '医生端' },
  { path: '/doctor/schedule', title: '值班查询', icon: 'List', group: '医生端' },
]

const adminMenus = [
  { path: '/admin/home', title: '首页概览', icon: 'HomeFilled', group: '管理员端' },     // 新增
  { path: '/admin/profile', title: '管理员个人信息', icon: 'UserFilled', group: '管理员端' },
  { path: '/admin/userManage', title: '账号权限管理', icon: 'List', group: '管理员端' },
  { path: '/admin/medicine', title: '药品管理', icon: 'UserFilled', group: '管理员端' },
  { path: '/admin/scheduling', title: '值班管理', icon: 'DataAnalysis', group: '管理员端' },
  { path: '/admin/ml/dashboard', title: 'AI 推理看板', icon: 'DataAnalysis', group: '管理员端' },
  { path: '/admin/ml/samples', title: '样本标注', icon: 'CollectionTag', group: '管理员端' },
  { path: '/admin/ml/models', title: '模型管理', icon: 'Cpu', group: '管理员端' },
  { path: '/admin/ml/ct-inference', title: 'CT 伪影检测', icon: 'Camera', group: '管理员端' },
]

// ==================== 角色判断（从 sessionStorage 获取）====================
const currentRole = ref<'doctor' | 'admin'>('doctor')

// 从 sessionStorage 读取角色
const getRoleFromSession = (): 'doctor' | 'admin' => {
  const role = sessionStorage.getItem('userRole')
  if (role === 'admin') return 'admin'
  return 'doctor'
}

// 根据当前路由路径自动更新角色
const updateRoleFromPath = () => {
  if (route.path.startsWith('/admin')) {
    currentRole.value = 'admin'
  } else if (route.path.startsWith('/doctor')) {
    currentRole.value = 'doctor'
  } else {
    // 首页等通用路径，保持已有角色或从 sessionStorage 读取
    currentRole.value = getRoleFromSession()
  }
}

// 监听路由变化，自动切换角色
watch(() => route.path, () => {
  updateRoleFromPath()
}, { immediate: true })

// 根据角色动态计算菜单项
const menuItems = computed(() => {
  return currentRole.value === 'doctor' ? doctorMenus : adminMenus
})

// 判断是否显示分组标签
function showGroupLabel(item: { path: string; title: string; icon: string; group: string }) {
  const items = menuItems.value
  const idx = items.findIndex(i => i.path === item.path)
  if (idx === -1) return false
  return idx > 0 && items[idx - 1]!.group !== item.group
}
</script>

<style>
/* 样式保持你原来的不变 */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');

:root {
  --sidebar-w: 240px;
  --brand: #2563eb;
  --brand-light: #3b82f6;
  --brand-dark: #1d4ed8;
  --surface: #ffffff;
  --bg: #f1f5f9;
  --text: #1e293b;
  --text-muted: #64748b;
  --border: #e2e8f0;
  --radius: 12px;
  --shadow: 0 1px 3px rgba(0,0,0,.06), 0 1px 2px rgba(0,0,0,.04);
  --shadow-md: 0 4px 6px rgba(0,0,0,.04), 0 2px 4px rgba(0,0,0,.04);
}

* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif; background: var(--bg); color: var(--text); -webkit-font-smoothing: antialiased; }
#app { width: 100%; height: 100vh; }
</style>

<style scoped>
.app-shell { display: flex; height: 100vh; overflow: hidden; }

.sidebar {
  width: var(--sidebar-w); min-width: var(--sidebar-w);
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  display: flex; flex-direction: column;
  transition: width .25s ease, min-width .25s ease;
  position: relative; z-index: 10;
}
.sidebar.collapsed { width: 68px; min-width: 68px; }

.brand {
  display: flex; align-items: center; gap: 10px;
  padding: 22px 20px; cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,.06);
}
.brand-icon { flex-shrink: 0; }
.brand-text { font-size: 18px; font-weight: 700; color: #f1f5f9; letter-spacing: -.3px; white-space: nowrap; }
.brand-accent { color: #60a5fa; }

.nav { flex: 1; padding: 12px 10px; overflow-y: auto; display: flex; flex-direction: column; gap: 2px; }
.nav-group-label { font-size: 11px; font-weight: 600; color: #64748b; text-transform: uppercase; letter-spacing: .5px; padding: 12px 12px 4px; margin-top: 4px; }
.nav-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: 8px;
  color: #94a3b8; text-decoration: none;
  font-size: 14px; font-weight: 500;
  transition: all .15s ease; white-space: nowrap;
  position: relative;
}
.nav-item:hover { background: rgba(255,255,255,.06); color: #e2e8f0; }
.nav-item.router-link-active { background: rgba(37,99,235,.25); color: #bfdbfe; }
.nav-item.router-link-active::before { content: ''; position: absolute; left: 0; top: 8px; bottom: 8px; width: 3px; background: var(--brand); border-radius: 0 3px 3px 0; }

.sidebar-footer {
  padding: 10px 20px; border-top: 1px solid rgba(255,255,255,.06);
  display: flex; flex-direction: column; gap: 8px;
  font-size: 12px; color: #64748b;
}
.footer-status { display: flex; align-items: center; gap: 8px; }
.status-dot { width: 7px; height: 7px; border-radius: 50%; background: #22c55e; box-shadow: 0 0 6px rgba(34,197,94,.5); }
.logout-btn {
  justify-content: flex-start; width: 100%; color: #94a3b8; font-size: 13px; padding: 6px 8px; border-radius: 8px;
}
.logout-btn:hover { color: #ef4444; background: rgba(239,68,68,.12); }

.main { flex: 1; overflow-y: auto; overflow-x: hidden; background: var(--bg); }
</style>