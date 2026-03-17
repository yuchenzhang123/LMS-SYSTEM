import axios from 'axios'
import { Message } from 'element-ui'
import { APP_CONFIG } from '@/config'
import { redirectToExternalLogin } from './cookie'

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
    const res = response.data
    if (res.code === '0' || res.code === 0) {
      return res
    } else {
      if (!APP_CONFIG.LOCAL_MENU_MODE) {
        Message.error(res.message || '权限校验失败')
        redirectToExternalLogin()
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  error => {
    if (!APP_CONFIG.LOCAL_MENU_MODE) {
      if (error.response && (error.response.status === 401 || error.response.status === 403)) {
        redirectToExternalLogin()
      }
    }
    return Promise.reject(error)
  }
)

export default service
