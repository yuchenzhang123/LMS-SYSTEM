import { APP_CONFIG } from '@/config'

export function getCookie() {
  console.log('[Cookie] 开始获取Cookie')
  console.log('[Cookie] Cookie名称:', APP_CONFIG.COOKIE_NAME)
  console.log('[Cookie] 完整Cookie:', document.cookie)

  const reg = new RegExp('(^| )' + APP_CONFIG.COOKIE_NAME + '=([^;]*)(;|$)')
  const arr = document.cookie.match(reg)

  if (arr) {
    const cookieValue = unescape(arr[2])
    console.log('[Cookie] ✓ 找到Cookie:', cookieValue)
    return cookieValue
  } else {
    console.log('[Cookie] ✗ 未找到Cookie')
    return null
  }
}

export function redirectToExternalLogin() {
  console.log('[跳转登录] 准备跳转到登录页')
  console.log('[跳转登录] 当前URL:', window.location.href)
  console.log('[跳转登录] 登录URL:', APP_CONFIG.EXTERNAL_LOGIN_URL)

  // 清除 Cookie
  document.cookie = `${APP_CONFIG.COOKIE_NAME}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=${APP_CONFIG.COOKIE_DOMAIN}`
  console.log('[跳转登录] 已清除Cookie')

  // 开发环境下如果不希望被强制跳转，可以在这里加判断
  if (APP_CONFIG.LOCAL_MENU_MODE) {
    console.warn('[跳转登录] 开发模式：拦截到未登录状态，已阻止跳转外部登录页')
    return
  }

  if (process.env.VUE_APP_DISABLE_LOGIN_REDIRECT) {
    console.warn('[跳转登录] 已禁用登录跳转（VUE_APP_DISABLE_LOGIN_REDIRECT=true）')
    return
  }

  const currentUrl = window.location.href
  const redirectUrl = APP_CONFIG.EXTERNAL_LOGIN_URL + encodeURIComponent(currentUrl)
  console.log('[跳转登录] 即将跳转到:', redirectUrl)

  window.location.href = redirectUrl
}
