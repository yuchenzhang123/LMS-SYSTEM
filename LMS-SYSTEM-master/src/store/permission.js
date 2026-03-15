import { validateTokenCheck, getDynamicMenusApi } from '@/api/auth'
import { ALL_DEV_MENUS } from '@/config/dev-menus'
import { loadView } from '@/utils/dynamic-loader'
import router from '@/router'

const state = {
  hasValidated: false,
  userInfo: null,
  menus: []
}

const mutations = {
  SET_AUTH: (state, info) => {
    state.userInfo = info
    state.hasValidated = true
  },
  SET_MENUS: (state, menus) => {
    state.menus = menus
  },
  RESET_STATE: (state) => {
    state.hasValidated = false
    state.userInfo = null
    state.menus = []
  }
}

const actions = {
  async initAuth({ commit }) {
    try {
      let finalUserInfo = null
      let rawMenuData = []

      // 开发环境：加载你提供的 ALL_DEV_MENUS
      if (process.env.NODE_ENV === 'development' && process.env.VUE_APP_SHOW_ALL_MENUS === 'true') {
        finalUserInfo = { userName: '开发管理员', userId: '954' }
        rawMenuData = ALL_DEV_MENUS
      } else {
        // 生产环境
        const ssoRes = await validateTokenCheck()
        finalUserInfo = ssoRes.userInfo
        rawMenuData = await getDynamicMenusApi(finalUserInfo.userId)
      }

      // 生成路由
      const dynamicRoutes = generateRoutes(rawMenuData)

      // 核心：将动态生成的路由全部挂载到 Layout (路径为 '/') 的 children 下
      dynamicRoutes.forEach(route => {
        router.addRoute('RootLayout', route)
      })
      if (typeof router.hasRoute !== 'function' || !router.hasRoute('NotFoundRoute')) {
        router.addRoute({
          path: '*',
          name: 'NotFoundRoute',
          redirect: '/404'
        })
      }

      commit('SET_AUTH', finalUserInfo)
      commit('SET_MENUS', rawMenuData)

      return { userInfo: finalUserInfo, menus: rawMenuData }
    } catch (error) {
      commit('RESET_STATE')
      console.error('权限初始化失败:', error)
      throw error
    }
  },

  resetState({ commit }) {
    commit('RESET_STATE')
  }
}

/**
 * 辅助函数：将菜单树转为路由树
 */
function generateRoutes(menus) {
  const routes = []
  menus.forEach(item => {
    const rawUrl = item.modelUrl || ''
    const routePath = rawUrl.startsWith('/') ? rawUrl : `/${rawUrl}`
    const hasChildren = item.children && item.children.length > 0
    if (hasChildren) {
      routes.push(...generateRoutes(item.children))
    } else {
      const viewPath = rawUrl.startsWith('/') ? rawUrl.slice(1) : rawUrl
      routes.push({
        path: routePath,
        name: item.modelId,
        meta: {
          title: item.modelName,
          icon: item.parameter
        },
        component: loadView(viewPath)
      })
    }
  })
  return routes
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
