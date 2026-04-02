const parseBoolean = (value, defaultValue) => {
  if (typeof value === 'undefined') {
    return defaultValue
  }
  return String(value).toLowerCase() === 'true'
}

const ssoGuardEnabled = parseBoolean(process.env.VUE_APP_ENABLE_SSO_GUARD, true)

export const APP_CONFIG = {
  EXTERNAL_LOGIN_URL: process.env.VUE_APP_SSO_LOGIN_URL,
  SSO_API_URL: process.env.VUE_APP_SSO_PREFIX,
  MENU_API_URL: process.env.VUE_APP_API_PREFIX,
  SSO_GUARD_ENABLED: ssoGuardEnabled,
  LOCAL_MENU_MODE: !ssoGuardEnabled
}
