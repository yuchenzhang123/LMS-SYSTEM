const parseBoolean = (value, defaultValue) => {
  if (typeof value === 'undefined') {
    return defaultValue
  }
  return String(value).toLowerCase() === 'true'
}

// 校验URL前缀，必须为 / 开头相对路径 或 http(s):// 开头绝对URL
const validateUrlPrefix = (value, name) => {
  if (!value || typeof value !== 'string') return ''
  if (/^(https?:\/\/|\/)/.test(value)) return value
  console.error(`[Config] 拒绝不安全的${name}: ${value}`)
  return ''
}

const ssoGuardEnabled = parseBoolean(process.env.VUE_APP_ENABLE_SSO_GUARD, true)

export const APP_CONFIG = {
  EXTERNAL_LOGIN_URL: validateUrlPrefix(process.env.VUE_APP_SSO_LOGIN_URL, 'SSO_LOGIN_URL'),
  SSO_API_URL: validateUrlPrefix(process.env.VUE_APP_SSO_PREFIX, 'SSO_PREFIX'),
  OAUTH_URL: validateUrlPrefix(process.env.VUE_APP_OAUTH_PREFIX, 'OAUTH_PREFIX'),
  API_URL: validateUrlPrefix(process.env.VUE_APP_API_PREFIX, 'API_PREFIX'),
  COOKIE_DOMAIN: process.env.VUE_APP_COOKIE_DOMAIN || '',
  // SSO/外部服务用的 OAuth client
  OAUTH_CLIENT_ID: process.env.VUE_APP_OAUTH_CLIENT_ID,
  OAUTH_CLIENT_SECRET: process.env.VUE_APP_OAUTH_CLIENT_SECRET,
  SYS_ID: process.env.VUE_APP_SYS_ID,
  SSO_GUARD_ENABLED: ssoGuardEnabled,
  LOCAL_MENU_MODE: !ssoGuardEnabled
}
