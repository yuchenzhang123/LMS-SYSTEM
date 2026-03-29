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
    tokenCheckPromise = tokenCheckClient({
      url: APP_CONFIG.SSO_API_URL + '/sso/tokenCheck',
      method: 'post',
      data: {}
    }).then(response => {
      const res = response && response.data ? response.data : {}
      if (res.code === '0' || res.code === 0) {
        // 更新用户信息到store（不改变hasValidated状态，菜单加载由initAuth负责）
        const userInfo = res.SSOLoginResponse?.userInfo
        if (userInfo && store && store.state && store.state.permission) {
          store.commit('permission/SET_USER_INFO', userInfo)
        }
        return true
      }
      throw new Error(res.message || '登录状态已失效')
    }).catch(error => {
      // 开发环境下如果SSO服务器不可用，跳过重定向
      if (APP_CONFIG.LOCAL_MENU_MODE) {
        console.warn('开发环境: SSO服务器不可用，跳过token校验')
        return true
      }
      redirectToExternalLogin()
      throw error
    }).finally(() => {
      tokenCheckPromise = null
    })
  }
  return tokenCheckPromise
}

service.interceptors.request.use(
  async config => {
    if (!shouldBypassTokenCheck() && !isTokenCheckRequest(config)) {
      await ensureTokenValid()
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
        Message.error(errorMessage)
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
        Message.error(errorMessage)
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
