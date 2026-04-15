const parseBoolean = (value, defaultValue) => {
  if (typeof value === 'undefined') {
    return defaultValue
  }
  return String(value).toLowerCase() === 'true'
}

const ssoGuardEnabled = parseBoolean(process.env.VUE_APP_ENABLE_SSO_GUARD, true)

export const APP_CONFIG = {
  EXTERNAL_LOGIN_URL: process.env.VUE_APP_SSO_LOGIN_URL,
  SSO_API_URL: process.env.VUE_APP_SSO_PREFIX,        // SSO 认证服务前缀 → /ssoservice
  OAUTH_URL: process.env.VUE_APP_OAUTH_PREFIX,        // OAuth2 令牌服务前缀 → /oauth
  API_URL: process.env.VUE_APP_API_PREFIX,            // 业务后端 + 外部模型服务 → /api
  COOKIE_DOMAIN: process.env.VUE_APP_COOKIE_DOMAIN || '',
  // SSO/外部服务用的 OAuth client
  OAUTH_CLIENT_ID: process.env.VUE_APP_OAUTH_CLIENT_ID,
  OAUTH_CLIENT_SECRET: process.env.VUE_APP_OAUTH_CLIENT_SECRET,
  SYS_ID: process.env.VUE_APP_SYS_ID,
  SSO_GUARD_ENABLED: ssoGuardEnabled,
  LOCAL_MENU_MODE: !ssoGuardEnabled
}
