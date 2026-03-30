import Vue from 'vue'
import Router from 'vue-router'
import store from '@/store'
import Layout from '@/layout/index.vue'
import { APP_CONFIG } from '@/config'
import { getCookie, redirectToExternalLogin } from '@/utils/cookie'

Vue.use(Router)

export const constantRoutes = [
  {
    path: '/',
    name: 'RootLayout',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'collection/account-detail',
        name: 'AccountDetail',
        component: () => import('@/views/collection/account-detail.vue'),
        meta: { title: '催收详情' }
      },
      {
        path: 'notice/list',
        name: 'NoticeList',
        component: () => import('@/views/notice/list.vue'),
        meta: { title: '消息通知' }
      },
      {
        path: 'notice/detail',
        name: 'NoticeDetail',
        component: () => import('@/views/notice/detail.vue'),
        meta: { title: '通知详情' }
      }
    ]
  },
  {
    path: '/404',
    component: () => import('@/views/error-page/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = new Router({
  mode: 'history',
  base: process.env.BASE_URL,
  scrollBehavior: () => ({ y: 0 }),
  routes: constantRoutes
})

router.beforeEach(async (to, from, next) => {
  // 1. 开发环境免拦截
  if (!store || !store.state || !store.state.permission) {
    console.warn('Store 尚未就绪，跳过本次守卫')
    return next()
  }

  const { permission } = store.state

  // --- 开发环境逻辑 ---
  if (APP_CONFIG.LOCAL_MENU_MODE) {
    if (!permission.hasValidated) {
      await store.dispatch('permission/initAuth')
      // 动态添加路由后，必须用 next({ ...to, replace: true }) 重新进入
      return next({ ...to, replace: true })
    }
    return next()
  }
  // 2. 生产环境逻辑
  const hasCookie = getCookie()
  if (!hasCookie) {
    redirectToExternalLogin()
    return
  }

  if (store.state.permission.hasValidated) {
    next()
  } else {
    try {
      await store.dispatch('permission/initAuth')
      next({ ...to, replace: true })
    } catch (e) {
      console.error('权限初始化失败:', e.message)
      // 认证失败时，ensureTokenValid已经调用了redirectToExternalLogin
      // 这里不需要额外处理，等待页面跳转
    }
  }
})

export default router
