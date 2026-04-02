import { APP_CONFIG } from '@/config'

export function redirectToExternalLogin() {
  console.log('[跳转登录] 准备跳转到登录页')
  console.log('[跳转登录] 当前URL:', window.location.href)
  console.log('[跳转登录] 登录URL:', APP_CONFIG.EXTERNAL_LOGIN_URL)

  // 注意：使用原生Cookie行为，Cookie管理由浏览器和SSO系统处理
  // 不需要手动清除特定Cookie

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
