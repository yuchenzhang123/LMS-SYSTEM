import { APP_CONFIG } from '@/config'

export function getCookie() {
  const reg = new RegExp('(^| )' + APP_CONFIG.COOKIE_NAME + '=([^;]*)(;|$)')
  const arr = document.cookie.match(reg)
  return arr ? unescape(arr[2]) : null
}

export function redirectToExternalLogin() {
  // 清除 Cookie
  document.cookie = `${APP_CONFIG.COOKIE_NAME}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=${APP_CONFIG.COOKIE_DOMAIN}`

  // 开发环境下如果不希望被强制跳转，可以在这里加判断
  if (APP_CONFIG.LOCAL_MENU_MODE) {
    console.warn('开发模式：拦截到未登录状态，已阻止跳转外部登录页')
    return
  }

  const currentUrl = window.location.href
  window.location.href = APP_CONFIG.EXTERNAL_LOGIN_URL + encodeURIComponent(currentUrl)
}
