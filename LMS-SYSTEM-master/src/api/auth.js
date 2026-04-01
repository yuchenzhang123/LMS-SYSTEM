import request from '@/utils/request'
import { APP_CONFIG } from '@/config'
import { getCookie } from '@/utils/cookie'

// 1. Token 校验接口 (8098 端口)
export function validateTokenCheck() {
  const token = getCookie()
  return request({
    url: APP_CONFIG.SSO_API_URL + '/sso/tokenCheck',
    method: 'post',
    headers: token ? {
      'Cookie': `${APP_CONFIG.SSO_COOKIE_NAME}=${token}`
    } : {},
    data: {} // RequestBody 可不送
  })
}

// 2. 获取动态菜单接口 (8099 端口)
export function getDynamicMenusApi(userId) {
  return request({
    url: `${APP_CONFIG.MENU_API_URL}/model/menulist/${userId}`,
    method: 'get',
    params: {
      sysId: 'GDLWXT'
    }
  })
}