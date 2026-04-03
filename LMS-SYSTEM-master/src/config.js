const parseBoolean = (value, defaultValue) => {
  if (typeof value === 'undefined') {
    return defaultValue
  }
  return String(value).toLowerCase() === 'true'
}

const ssoGuardEnabled = parseBoolean(process.env.VUE_APP_ENABLE_SSO_GUARD, true)

export const APP_CONFIG = {
  EXTERNAL_LOGIN_URL: process.env.VUE_APP_SSO_LOGIN_URL,
  SSO_API_URL: process.env.VUE_APP_SSO_PREFIX,       // 外部 SSO 服务 → /ssoservice
  MODEL_API_URL: process.env.VUE_APP_MODEL_PREFIX,   // 外部菜单/模型服务 → /modelservice
  API_URL: process.env.VUE_APP_API_PREFIX,           // 本地业务后端 → /api
  SYS_ID: process.env.VUE_APP_SYS_ID,
  SSO_GUARD_ENABLED: ssoGuardEnabled,
  LOCAL_MENU_MODE: !ssoGuardEnabled
}
