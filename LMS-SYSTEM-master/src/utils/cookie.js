import { APP_CONFIG } from '@/config'

// 清除所有前端可访问的 Cookie（HttpOnly Cookie 由服务端控制，无法从 JS 清除）
export function clearCookies() {
  const cookies = document.cookie.split(';')
  const expired = 'expires=Thu, 01 Jan 1970 00:00:00 GMT'
  const configDomain = APP_CONFIG.COOKIE_DOMAIN // e.g. ".hi.bank-of-china.com"
  // 始终尝试：配置域（若有）、当前 hostname、不指定域
  const domains = ['', window.location.hostname]
  if (configDomain) domains.unshift(configDomain)

  cookies.forEach(cookie => {
    const name = cookie.split('=')[0].trim()
    if (!name) return
    domains.forEach(domain => {
      const domainPart = domain ? `;domain=${domain}` : ''
      document.cookie = `${name}=;${expired};path=/${domainPart}`
    })
  })
}

export function redirectToExternalLogin() {
  console.log('[跳转登录] 准备跳转到登录页')
  console.log('[跳转登录] 当前URL:', window.location.href)
  console.log('[跳转登录] 登录URL:', APP_CONFIG.EXTERNAL_LOGIN_URL)

  if (APP_CONFIG.LOCAL_MENU_MODE) {
    console.warn('[跳转登录] 开发模式：拦截到未登录状态，已阻止跳转外部登录页')
    return
  }

  if (process.env.VUE_APP_DISABLE_LOGIN_REDIRECT) {
    console.warn('[跳转登录] 已禁用登录跳转（VUE_APP_DISABLE_LOGIN_REDIRECT=true）')
    return
  }

  const loc = window.location
  const currentUrl = loc.protocol + '//' + loc.hostname + (loc.port ? ':' + loc.port : '')
  // 白名单校验 currentUrl，仅允许 https?://域名[:端口] 格式，防止URL注入
  if (!/^https?:\/\/[a-zA-Z0-9.-]+(:\d+)?$/.test(currentUrl)) {
    console.error('[跳转登录] 非法的当前URL，已阻止跳转:', currentUrl)
    return
  }
  const redirectUrl = APP_CONFIG.EXTERNAL_LOGIN_URL + '/?from=' + currentUrl
  console.log('[跳转登录] 跳转到:', redirectUrl)

  // 校验协议，防止 javascript: 等 XSS 攻击
  if (!/^https?:\/\//i.test(redirectUrl)) {
    console.error('[跳转登录] 非法的重定向URL，已阻止跳转:', redirectUrl)
    return
  }
  window.location.assign(redirectUrl)
}
