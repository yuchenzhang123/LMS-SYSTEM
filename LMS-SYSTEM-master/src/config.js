export const APP_CONFIG = {
  // 核心 Cookie 名称
  COOKIE_NAME: 'WSSOP_SSO_COOKIE_V1',
  // 外部 SSO 登录地址
  EXTERNAL_LOGIN_URL: 'http://22.156.22.42:8098/login?redirect=',
  // SSO 校验服务地址 (对应 8098 端口)
  SSO_API_URL: '/ssoservice',
  // 菜单服务地址 (对应 8099 端口)
  MENU_API_URL: '/api',
  // Cookie 作用域
  COOKIE_DOMAIN: '.hi.bank-of-china.com'
}