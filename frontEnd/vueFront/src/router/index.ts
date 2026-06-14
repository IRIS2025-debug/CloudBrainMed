import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      name: 'DoctorHome',
      component: () => import('@/pages/HomeView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/doctor/profile',
      name: 'doctorProfile',
      component: () => import('@/pages/doctor/profile/Index.vue'),
      meta: { requiresAuth: true, role: 2 }
    },
    {
      path: '/doctor/consult',
      name: 'doctorConsult',
      component: () => import('@/pages/doctor/consult/List.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/doctor/consult/:registerId',
      name: 'doctorConsultDetail',
      component: () => import('@/pages/doctor/consult/Detail.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/doctor/ai-medicine',
      name: 'aiMedicine',
      component: () => import('@/pages/doctor/ai-medicine/Index.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/doctor/schedule',
      name: 'doctorSchedule',
      component: () => import('@/pages/doctor/schedule/Schedule.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin/ml/dashboard',
      name: 'mlDashboard',
      component: () => import('@/pages/admin/ml/Dashboard.vue'),
      meta: { requiresAuth: true, role: 3 }
    },
    {
      path: '/admin/ml/samples',
      name: 'mlSamples',
      component: () => import('@/pages/admin/ml/Samples.vue'),
      meta: { requiresAuth: true, role: 3 }
    },
    {
      path: '/admin/ml/models',
      name: 'mlModels',
      component: () => import('@/pages/admin/ml/Models.vue'),
      meta: { requiresAuth: true, role: 3 }
    },
    {
      path: '/admin/ml/ct-inference',
      name: 'ctInference',
      component: () => import('@/pages/admin/ml/CTInference.vue'),
      meta: { requiresAuth: true, role: 3 }
    },
    {
      path: '/admin/scheduling',
      name: 'scheduling',
      component: () => import('@/pages/admin/scheduling/Scheduling.vue'),
      meta: { requiresAuth: true, role: 3 }
    },

    {
      path: '/admin/medicine',
      name: 'AdminMedicine',
      component: () => import('@/pages/admin/medicine/AdminMedicine.vue'),
      meta: { requiresAuth: true, role: 3 }
    },
    {
      path: '/admin/profile',
      name: 'AdminProfile',
      component: () => import('@/pages/admin/profile/AdminProfile.vue'),
      meta: { requiresAuth: true, role: 3 }
    },
    {
      path: '/admin/userManage',
      name: 'AdminUserManage',
      component: () => import('@/pages/admin/userManage/AdminUserManage.vue'),
      meta: { requiresAuth: true, role: 3 }
    },
    {
      path: '/admin/home',
      name: 'AdminHome',
      component: () => import('@/pages/admin/AdminHome.vue'),
      meta: { requiresAuth: true, role: 3 }
    },
  ],
})

// 路由守卫
router.beforeEach((to, from) => {
  const token = sessionStorage.getItem('token')
  const roleType = sessionStorage.getItem('roleType')

  // 如果访问登录页面，直接放行
  if (to.path === '/login') {
    return true
  }

  // 需要验证权限的路由
  if (to.meta.requiresAuth) {
    if (!token) {
      // 没有 token，跳转到登录页
      return '/login'
    }

    // 检查角色权限
    if (to.meta.role) {
      const userRole = parseInt(roleType || '0')
      if (userRole !== to.meta.role) {
        return '/login'
      }
    }
  }

  return true
})

export default router