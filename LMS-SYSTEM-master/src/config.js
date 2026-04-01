const parseBoolean = (value, defaultValue) => {
  if (typeof value === 'undefined') {
    return defaultValue
  }
  return String(value).toLowerCase() === 'true'
}

const ssoGuardEnabled = parseBoolean(process.env.VUE_APP_ENABLE_SSO_GUARD, true)

export const APP_CONFIG = {
  // 实际登录后设置的Cookie名称（用于获取token）
  COOKIE_NAME: process.env.VUE_APP_COOKIE_NAME || 'SSOToken',
  // 登录后额外设置的SSO Cookie（可能用于其他用途）
  SSO_COOKIE_NAME: 'WSSOP_SSO_COOKIE_V1',
  EXTERNAL_LOGIN_URL: process.env.VUE_APP_SSO_LOGIN_URL,
  SSO_API_URL: process.env.VUE_APP_SSO_PREFIX,
  MENU_API_URL: process.env.VUE_APP_API_PREFIX,
  SSO_GUARD_ENABLED: ssoGuardEnabled,
  LOCAL_MENU_MODE: !ssoGuardEnabled,
  COOKIE_DOMAIN: process.env.VUE_APP_COOKIE_DOMAIN
}
