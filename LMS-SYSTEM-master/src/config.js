const parseBoolean = (value, defaultValue) => {
  if (typeof value === 'undefined') {
    return defaultValue
  }
  return String(value).toLowerCase() === 'true'
}

const ssoGuardEnabled = parseBoolean(process.env.VUE_APP_ENABLE_SSO_GUARD, true)

export const APP_CONFIG = {
  // 从浏览器读取Cookie时使用的名称
  COOKIE_NAME: process.env.VUE_APP_COOKIE_NAME || 'SSOToken',
  // 发送到SSO服务器时请求头中使用的Cookie名称
  SSO_COOKIE_NAME: 'WSSOP_SSO_COOKIE_V1',
  EXTERNAL_LOGIN_URL: process.env.VUE_APP_SSO_LOGIN_URL,
  SSO_API_URL: process.env.VUE_APP_SSO_PREFIX,
  MENU_API_URL: process.env.VUE_APP_API_PREFIX,
  SSO_GUARD_ENABLED: ssoGuardEnabled,
  LOCAL_MENU_MODE: !ssoGuardEnabled,
  COOKIE_DOMAIN: process.env.VUE_APP_COOKIE_DOMAIN
}
