import axios from 'axios'
import { Message } from 'element-ui'
import { APP_CONFIG } from '@/config'
import { redirectToExternalLogin } from './cookie'
import store from '@/store'

const service = axios.create({
  withCredentials: true,
  timeout: 15000
})

const tokenCheckClient = axios.create({
  withCredentials: true,
  timeout: 15000
})

let tokenCheckPromise = null

function shouldBypassTokenCheck () {
  return APP_CONFIG.LOCAL_MENU_MODE
}

function isTokenCheckRequest (config) {
  const requestUrl = config && config.url ? String(config.url) : ''
  return requestUrl.includes(`${APP_CONFIG.SSO_API_URL}/sso/tokenCheck`) || requestUrl.includes('/sso/tokenCheck')
}

function ensureTokenValid () {
  if (!tokenCheckPromise) {
    console.log('[Token校验] 开始验证（使用原生Cookie行为）')
    console.log('[Token校验] SSO地址:', APP_CONFIG.SSO_API_URL)

    tokenCheckPromise = tokenCheckClient({
      url: APP_CONFIG.SSO_API_URL + '/sso/tokenCheck',
      method: 'post',
      data: {}
    }).then(response => {
      console.log('[Token校验] 响应数据:', response)
      const res = response && response.data ? response.data : {}
      console.log('[Token校验] 响应解析后:', res)
      console.log('[Token校验] 响应code:', res.code, '响应message:', res.message)

      if (res.code === '0' || res.code === 0) {
        console.log('[Token校验] ✓ Token有效')
        // 更新用户信息到store（不改变hasValidated状态，菜单加载由initAuth负责）
        const userInfo = res.SSOLoginResponse?.userInfo
        if (userInfo && store && store.state && store.state.permission) {
          store.commit('permission/SET_USER_INFO', userInfo)
        }
        return true
      }
      // token校验失败，跳转登录页
      console.warn('[Token校验] ✗ Token校验失败:', res.message)
      console.warn('[Token校验] 完整响应:', JSON.stringify(res))
      if (!APP_CONFIG.LOCAL_MENU_MODE) {
        // 使用setTimeout确保错误被处理后跳转
        setTimeout(() => {
          console.log('[Token校验] 准备跳转登录页...')
          // 开发环境下，设置环境变量DISABLE_LOGIN_REDIRECT=true可以阻止跳转
          if (!process.env.VUE_APP_DISABLE_LOGIN_REDIRECT) {
            redirectToExternalLogin()
          } else {
            console.log('[Token校验] 已禁用登录跳转（VUE_APP_DISABLE_LOGIN_REDIRECT=true）')
          }
        }, 100)
      }
      return false
    }).catch(error => {
      console.error('[Token校验] ✗ Token校验请求失败:', error)
      console.error('[Token校验] 错误消息:', error.message)
      console.error('[Token校验] 错误堆栈:', error.stack)
      if (error.response) {
        console.error('[Token校验] 响应状态:', error.response.status)
        console.error('[Token校验] 响应头:', error.response.headers)
        console.error('[Token校验] 响应数据:', error.response.data)
      }

      // 开发环境下如果SSO服务器不可用，跳过重定向
      if (APP_CONFIG.LOCAL_MENU_MODE) {
        console.warn('[Token校验] 开发环境: SSO服务器不可用，跳过token校验')
        return true
      }
      // 网络错误等，跳转登录页
      if (!APP_CONFIG.LOCAL_MENU_MODE) {
        setTimeout(() => {
          console.log('[Token校验] 准备跳转登录页...')
          if (!process.env.VUE_APP_DISABLE_LOGIN_REDIRECT) {
            redirectToExternalLogin()
          } else {
            console.log('[Token校验] 已禁用登录跳转（VUE_APP_DISABLE_LOGIN_REDIRECT=true）')
          }
        }, 100)
      }
      return false
    }).finally(() => {
      tokenCheckPromise = null
    })
  }
  return tokenCheckPromise
}

service.interceptors.request.use(
  async config => {
    if (!shouldBypassTokenCheck() && !isTokenCheckRequest(config)) {
      const isValid = await ensureTokenValid()
      if (!isValid) {
        // 认证失败，返回一个被拒绝的Promise，请求不会发出
        return Promise.reject(new Error('认证失败，正在跳转登录页'))
      }
    }
    return config
  },
  error => Promise.reject(error)
)

  service.interceptors.response.use(
  response => {
    // 如果是 blob 响应（文件下载），需要检查是否为错误响应
    if (response.config.responseType === 'blob') {
      const contentType = response.headers['content-type'] || ''
      // 如果返回的是 JSON，说明是错误响应
      if (contentType.includes('application/json')) {
        return new Promise((_resolve, reject) => {
          const reader = new FileReader()
          reader.onload = () => {
            try {
              const errorData = JSON.parse(reader.result)
              const errorMessage = errorData.message || '操作失败'
              if (!APP_CONFIG.LOCAL_MENU_MODE) {
                Message.error(errorMessage)
              }
              reject(new Error(errorMessage))
            } catch (e) {
              reject(new Error('响应解析失败'))
            }
          }
          reader.onerror = () => reject(new Error('响应读取失败'))
          reader.readAsText(response.data)
        })
      }
      return response.data
    }

    const res = response.data
    if (res.code === '0' || res.code === 0) {
      return res
    } else {
      const errorMessage = res.message || '操作失败'
      // 权限相关错误（1002: 未授权, 1003: 未登录）才跳转登录页
      const isAuthError = res.code === '1002' || res.code === '1003'
      if (!APP_CONFIG.LOCAL_MENU_MODE && isAuthError) {
        redirectToExternalLogin()
      } else if (!APP_CONFIG.LOCAL_MENU_MODE) {
        Message.error(errorMessage)
      }
      return Promise.reject(new Error(errorMessage))
    }
  },
  error => {
    let errorMessage = '网络请求失败'
    if (error.response) {
      // 服务器返回了错误状态码
      const data = error.response.data
      const contentType = error.response.headers['content-type'] || ''

      // 检查响应是否为JSON格式
      if (contentType.includes('application/json') && data && data.message) {
        errorMessage = data.message
      } else if (error.response.status === 413) {
        // 413 Payload Too Large - 文件大小超过限制
        errorMessage = '文件大小超过限制，最大允许 10MB'
      } else if (error.response.status === 401 || error.response.status === 403) {
        errorMessage = '权限不足'
      } else if (typeof data === 'string' && data.includes('Maximum upload size exceeded')) {
        // 某些情况下Spring可能返回纯文本错误
        errorMessage = '文件大小超过限制，最大允许 10MB'
      }

      if (!APP_CONFIG.LOCAL_MENU_MODE && (error.response.status === 401 || error.response.status === 403)) {
        redirectToExternalLogin()
        return Promise.reject(new Error(errorMessage))
      }
    } else if (error.message) {
      errorMessage = error.message
    }
    if (!APP_CONFIG.LOCAL_MENU_MODE) {
      Message.error(errorMessage)
    }
    return Promise.reject(new Error(errorMessage))
  }
)

export default service
