import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/doctor/home',
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
      meta: { requiresAuth: true, role: 2 }
    },
    {
      path: '/doctor/consult/:registerId',
      name: 'doctorConsultDetail',
      component: () => import('@/pages/doctor/consult/Detail.vue'),
      meta: { requiresAuth: true, role: 2 }
    },
    {
      path: '/doctor/ai-medicine',
      name: 'aiMedicine',
      component: () => import('@/pages/doctor/ai-medicine/Index.vue'),
      meta: { requiresAuth: true, role: 2 }
    },
    {
      path: '/doctor/ai-exam-generate',
      name: 'aiExamGenerate',
      component: () => import('@/pages/doctor/ai-exam-generate/Index.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/doctor/schedule',
      name: 'doctorSchedule',
      component: () => import('@/pages/doctor/schedule/Schedule.vue'),
      meta: { requiresAuth: true, role: 2 }
    },
    {
      path: '/doctor/workbench',
      name: 'doctorWorkbench',
      component: () => import('@/pages/doctor/workbench/Index.vue'),
      meta: { requiresAuth: true, role: 2 }
    },
    {
      path: '/doctor/queue',
      name: 'doctorQueue',
      component: () => import('@/pages/doctor/queue/Index.vue'),
      meta: { requiresAuth: true, role: 2 }
    },
    {
      path: '/doctor/task/:id',
      name: 'doctorTaskDetail',
      component: () => import('@/pages/doctor/task-detail/Index.vue'),
      meta: { requiresAuth: true, role: 2 }
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
    {
      path: '/inspection-doctor/home',
      name: 'InspectionHome',
      component: () => import('@/pages/inspection-doctor/InspectionHome.vue'),
      meta: { requiresAuth: true, role: 2, doctorType: 3 }
    },
    {
      path: '/inspection-doctor/order-list',
      name: 'inspectionOrderList',
      component: () => import('@/pages/inspection-doctor/InspectionOrderList.vue'),
      meta: { requiresAuth: true, role: 2, doctorType: 3 }
    },
    {
      path: '/examination-doctor/home',
      name: 'ExaminationHome',
      component: () => import('@/pages/examination/ExaminationHome.vue'),
      meta: { requiresAuth: true, role: 2, doctorType: 2 }
    },
    {
      path: '/examination-doctor/ct-inference',
      name: 'ExaminationCTInference',
      component: () => import('@/pages/examination/CTInference.vue'),
      meta: { requiresAuth: true, role: 2, doctorType: 2 }
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

    if (to.path === '/') {
      if (roleType === '3') {
        return '/admin/home'
      }

      const doctorType = Number(sessionStorage.getItem('doctorType') || '1')
      if (doctorType === 2) {
        return '/examination-doctor/home'
      }
      if (doctorType === 3) {
        return '/inspection-doctor/home'
      }
    }

    // 检查角色权限
    if (to.meta.role) {
      const userRole = parseInt(roleType || '0')
      if (userRole !== to.meta.role) {
        return '/login'
      }
    }

    // 检查医生子角色（doctorType）权限
    if (to.meta.doctorType) {
      const userDoctorType = Number(sessionStorage.getItem('doctorType') || '0')
      if (userDoctorType !== to.meta.doctorType) {
        return '/login'
      }
    }
  }

  return true
})

export default router