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
        path: 'admin/account-list',
        name: 'AdminAccountList',
        component: () => import('@/views/admin/account-list.vue'),
        meta: { title: '账户总览' }
      },
      {
        path: 'org/hierarchy',
        name: 'OrgHierarchy',
        component: () => import('@/views/org/hierarchy.vue'),
        meta: { title: '机构层级管理' }
      },
      {
        path: 'collection/account-list',
        name: 'AccountList',
        component: () => import('@/views/collection/account-list.vue'),
        meta: { title: '个贷账户清单' }
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
  },
  {
    path: '/unauthorized',
    component: () => import('@/views/error-page/unauthorized.vue'),
    meta: { title: '暂无权限' }
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

  // 1. 无需守卫的白名单页面
  if (to.path === '/unauthorized' || to.path === '/404') {
    return next()
  }

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
      console.error('[路由守卫] 权限初始化失败:', e.message)
      if (e.message === 'ROLE_UNKNOWN') {
        console.warn('[路由守卫] 机构号无对应角色，跳转无权限页')
        return next('/unauthorized')
      }
      if (!APP_CONFIG.LOCAL_MENU_MODE) {
        console.log('[路由守卫] 准备跳转登录页...')
        redirectToExternalLogin()
      }
      return
    }
  }
})

export default router
