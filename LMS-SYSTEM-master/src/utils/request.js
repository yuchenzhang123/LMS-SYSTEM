import axios from 'axios'
import { Message } from 'element-ui'
import { redirectToExternalLogin } from './cookie'

const service = axios.create({
  withCredentials: true, // 必须开启以携带 Cookie
  timeout: 15000
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 根据业务需求，可以在这里添加特定的 Header，如 X-Real-IP (通常由 Nginx 处理)
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    // 对应接口文档：code "0" 为成功
    if (res.code === '0' || res.code === 0) {
      return res
    } else {
      // 只有非开发环境才执行踢人逻辑
      if (process.env.NODE_ENV !== 'development') {
        Message.error(res.message || '权限校验失败')
        redirectToExternalLogin()
      }
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  error => {
    if (process.env.NODE_ENV !== 'development') {
      if (error.response && (error.response.status === 401 || error.response.status === 403)) {
        redirectToExternalLogin()
      }
    }
    return Promise.reject(error)
  }
)

export default service