import axios from 'axios'
import { Message } from 'element-ui'
import { APP_CONFIG } from '@/config'
import { redirectToExternalLogin } from './cookie'
import { clearSession } from './session'
import store from '@/store'

const service = axios.create({
  withCredentials: true,
  timeout: 15000
})

// 注入 Authorization 令牌
// _needsToken: true → 外部服务令牌（SSO/模型接口）
service.interceptors.request.use(
  config => {
    if (config._needsToken) {
      const token = store.state.permission.accessToken
      if (token) {
        config.headers = config.headers || {}
        config.headers['Authorization'] = `Bearer ${token}`
      }
    }
    return config
  },
  error => Promise.reject(error)
)

function handleAuthFailure() {
  clearSession()
  store.commit('permission/RESET_STATE')
  redirectToExternalLogin()
}

service.interceptors.response.use(
  response => {
    // blob 响应（文件下载）处理
    if (response.config.responseType === 'blob') {
      const contentType = response.headers['content-type'] || ''
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

    // 标准 OAuth2 等原始响应，跳过 code 检查直接返回
    if (response.config._rawResponse) {
      return response.data
    }

    const res = response.data
    if (res.code === '0' || res.code === 0) {
      return res
    } else {
      const errorMessage = res.message || '操作失败'
      const isAuthError = res.code === '1002' || res.code === '1003'
      if (!APP_CONFIG.LOCAL_MENU_MODE && isAuthError) {
        handleAuthFailure()
      } else if (!APP_CONFIG.LOCAL_MENU_MODE) {
        Message.error(errorMessage)
      }
      const authErr = new Error(errorMessage)
      if (isAuthError) authErr.isAuthError = true
      return Promise.reject(authErr)
    }
  },
  async error => {
    const config = error.config || {}
    const status = error.response?.status

    // 令牌过期：首次 401 时尝试刷新令牌后重试一次
    if (status === 401 && config._needsToken && !config._tokenRetried) {
      config._tokenRetried = true
      try {
        const newToken = await store.dispatch('permission/refreshToken')
        config.headers = config.headers || {}
        config.headers['Authorization'] = `Bearer ${newToken}`
        return service(config)
      } catch {
        if (!APP_CONFIG.LOCAL_MENU_MODE) handleAuthFailure()
        return Promise.reject(error)
      }
    }

    let errorMessage = '网络请求失败'
    if (error.response) {
      const data = error.response.data
      const contentType = error.response.headers['content-type'] || ''

      if (contentType.includes('application/json') && data && data.message) {
        errorMessage = data.message
      } else if (status === 413) {
        errorMessage = '文件大小超过限制，最大允许 10MB'
      } else if (status === 401 || status === 403) {
        errorMessage = '权限不足'
      } else if (typeof data === 'string' && data.includes('Maximum upload size exceeded')) {
        errorMessage = '文件大小超过限制，最大允许 10MB'
      }

      if (!APP_CONFIG.LOCAL_MENU_MODE && (status === 401 || status === 403)) {
        handleAuthFailure()
        const authErr = new Error(errorMessage)
        authErr.isAuthError = true
        return Promise.reject(authErr)
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
