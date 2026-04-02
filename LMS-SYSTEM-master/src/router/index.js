import Vue from 'vue'
import Router from 'vue-router'
import store from '@/store'
import Layout from '@/layout/index.vue'
import { APP_CONFIG } from '@/config'
import { redirectToExternalLogin } from '@/utils/cookie'

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
  console.log('[路由守卫] 开始执行', { from: from.path, to: to.path })
  console.log('[路由守卫] LOCAL_MENU_MODE:', APP_CONFIG.LOCAL_MENU_MODE)
  console.log('[路由守卫] SSO_GUARD_ENABLED:', APP_CONFIG.SSO_GUARD_ENABLED)

  // 1. 开发环境免拦截
  if (!store || !store.state || !store.state.permission) {
    console.warn('[路由守卫] Store 尚未就绪，跳过本次守卫')
    return next()
  }

  const { permission } = store.state
  console.log('[路由守卫] hasValidated:', permission.hasValidated)
  console.log('[路由守卫] userInfo:', permission.userInfo)

  // --- 开发环境逻辑 ---
  if (APP_CONFIG.LOCAL_MENU_MODE) {
    console.log('[路由守卫] 开发环境模式')
    if (!permission.hasValidated) {
      console.log('[路由守卫] 初始化权限...')
      await store.dispatch('permission/initAuth')
      // 动态添加路由后，必须用 next({ ...to, replace: true }) 重新进入
      return next({ ...to, replace: true })
    }
    return next()
  }

  // 2. 生产环境逻辑
  console.log('[路由守卫] 生产环境模式')
  console.log('[路由守卫] 使用原生Cookie行为，浏览器会自动处理Cookie')

  // 注意：现在依赖请求拦截器中的token验证，这里不再手动检查Cookie

  if (store.state.permission.hasValidated) {
    console.log('[路由守卫] 已验证，继续导航')
    next()
  } else {
    console.log('[路由守卫] 初始化权限...')
    try {
      await store.dispatch('permission/initAuth')
      next({ ...to, replace: true })
    } catch (e) {
      console.error('[路由守卫] 权限初始化失败:', e)
      console.error('[路由守卫] 错误消息:', e.message)
      console.error('[路由守卫] 错误堆栈:', e.stack)
      // 权限初始化失败时，显示错误并跳转登录页
      if (!APP_CONFIG.LOCAL_MENU_MODE) {
        console.log('[路由守卫] 准备跳转登录页...')
        if (!process.env.VUE_APP_DISABLE_LOGIN_REDIRECT) {
          redirectToExternalLogin()
        } else {
          console.log('[路由守卫] 已禁用登录跳转（VUE_APP_DISABLE_LOGIN_REDIRECT=true）')
        }
      }
      return
    }
  }
})

export default router
