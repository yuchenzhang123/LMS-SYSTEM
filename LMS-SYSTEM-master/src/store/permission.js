import { validateTokenCheck, getAccessToken, getDynamicMenusApi, getOrgInfoApi } from '@/api/auth'
import { getRoleByOrgCodeApi } from '@/api/org'
import { ALL_DEV_MENUS, STAFF_MENUS, MANAGER_MENUS, ADMIN_MENUS } from '@/config/dev-menus'
import { loadView } from '@/utils/dynamic-loader'
import router from '@/router'
import { APP_CONFIG } from '@/config'
import { setSession, getSession, clearSession } from '@/utils/session'
import { redirectToExternalLogin, clearCookies } from '@/utils/cookie'

const state = {
  hasValidated: false,
  userInfo: null,
  menus: [],
  accessToken: null,
  tokenExpiresAt: null,
  orgCode: null,
  userRole: null
}

const mutations = {
  SET_AUTH: (state, info) => {
    state.userInfo = info
    state.hasValidated = true
  },
  SET_USER_INFO: (state, info) => {
    state.userInfo = info
  },
  SET_MENUS: (state, menus) => {
    state.menus = menus
  },
  SET_TOKEN: (state, { token, expiresAt }) => {
    state.accessToken = token
    state.tokenExpiresAt = expiresAt
  },
  SET_ORG: (state, { orgCode, userRole }) => {
    state.orgCode = orgCode
    state.userRole = userRole
  },
  RESET_STATE: (state) => {
    state.hasValidated = false
    state.userInfo = null
    state.menus = []
    state.accessToken = null
    state.tokenExpiresAt = null
    state.orgCode = null
    state.userRole = null
  }
}

const actions = {
  async initAuth({ commit }) {
    try {
      let finalUserInfo = null
      let rawMenuData = []

      if (APP_CONFIG.LOCAL_MENU_MODE) {
        finalUserInfo = { userName: '开发管理员', userId: '954' }
        // 开发模式：默认使用系统管理员菜单，可按需改为 MANAGER_MENUS / STAFF_MENUS
        rawMenuData = ADMIN_MENUS
        commit('SET_ORG', { orgCode: 'DEV_ADMIN', userRole: 'admin' })
      } else {
        // 1. 先检查本地 session（12小时有效期）
        const cachedUser = getSession()
        if (cachedUser) {
          console.log('[权限] 本地 session 有效，跳过 SSO 校验')
          finalUserInfo = cachedUser
        } else {
          // 2. session 不存在或已过期，进行 SSO 验证
          console.log('[权限] session 无效，进行 SSO 验证...')
          let ssoRes
          try {
            ssoRes = await validateTokenCheck()
          } catch (ssoError) {
            console.error('[权限] SSO 请求失败，清除 session 和 cookie')
            clearSession()
            clearCookies()
            throw ssoError
          }

          finalUserInfo = ssoRes.SSOLoginResponse?.userInfo
          if (!finalUserInfo || !finalUserInfo.userId) {
            console.error('[权限] SSO 验证失败，清除 session 和 cookie，返回数据:', ssoRes)
            clearSession()
            clearCookies()
            throw new Error('SSO返回数据格式错误，缺少userInfo')
          }

          // orgId 与 userName 同级，保存在 userInfo 中
          console.log('[权限] orgId:', finalUserInfo.orgId)
          setSession(finalUserInfo)
          console.log('[权限] SSO 验证通过，session 已建立（12小时）')
        }

        // 3. 获取访问令牌
        console.log('[权限] 获取访问令牌...')
        const tokenRes = await getAccessToken()
        const accessToken = tokenRes.access_token
        if (!accessToken) {
          throw new Error('获取访问令牌失败，响应数据：' + JSON.stringify(tokenRes))
        }
        const expiresAt = tokenRes.expires_in
          ? Date.now() + (tokenRes.expires_in - 30) * 1000
          : null
        commit('SET_TOKEN', { token: accessToken, expiresAt })
        console.log('[权限] 访问令牌已获取')

        // 4. 获取机构号和角色（orgId 来自 tokenCheck 的 userInfo）
        try {
          const orgId = finalUserInfo.orgId
          let orgCode = null
          if (orgId) {
            const orgInfoRes = await getOrgInfoApi(orgId)
            orgCode = orgInfoRes.ehrNo || orgInfoRes.data?.ehrNo || null
          }
          console.log('[权限] orgId:', orgId, '→ orgCode(ehrNo):', orgCode)
          let userRole = 'unknown'
          if (orgCode) {
            const roleRes = await getRoleByOrgCodeApi(orgCode)
            userRole = roleRes.data || roleRes || 'unknown'
          }
          commit('SET_ORG', { orgCode, userRole })
          console.log('[权限] 机构号:', orgCode, '角色:', userRole)
          // 根据角色决定使用哪套菜单
          if (userRole === 'admin') {
            rawMenuData = ADMIN_MENUS
          } else if (userRole === 'manager') {
            rawMenuData = MANAGER_MENUS
          } else if (userRole === 'staff') {
            rawMenuData = STAFF_MENUS
          } else {
            // unknown：机构号不在任何角色配置中，抛出无权限错误
            const err = new Error('ROLE_UNKNOWN')
            err.orgCode = orgCode
            throw err
          }
        } catch (orgError) {
          console.error('[权限] 获取机构号/角色失败，拒绝访问', orgError)
          throw orgError
        }

        // 5. rawMenuData 已由角色决定（步骤4），跳过动态菜单接口
      }

      // 生成并挂载动态路由
      const dynamicRoutes = generateRoutes(rawMenuData)
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

  // 刷新外部服务令牌（由 request.js 的 401 重试逻辑调用）
  async refreshToken({ commit, state }) {
    if (state.accessToken && state.tokenExpiresAt && Date.now() < state.tokenExpiresAt) {
      return state.accessToken
    }
    const tokenRes = await getAccessToken()
    const accessToken = tokenRes.access_token
    if (!accessToken) throw new Error('刷新访问令牌失败')
    const expiresAt = tokenRes.expires_in
      ? Date.now() + (tokenRes.expires_in - 30) * 1000
      : null
    commit('SET_TOKEN', { token: accessToken, expiresAt })
    console.log('[权限] 访问令牌已刷新')
    return accessToken
  },

  // 退出登录：清除 session，重置状态，跳转登录页
  logout({ commit }) {
    clearSession()
    clearCookies()
    commit('RESET_STATE')
    redirectToExternalLogin()
  }
}

function generateRoutes(menus) {
  const routes = []
  menus.forEach(item => {
    const rawUrl = item.modelUrl || ''
    const hasChildren = item.children && item.children.length > 0
    if (hasChildren) {
      routes.push(...generateRoutes(item.children))
    } else if (rawUrl) {
      const routePath = rawUrl.startsWith('/') ? rawUrl : `/${rawUrl}`
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
