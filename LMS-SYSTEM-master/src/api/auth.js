import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

// 1. SSO Token 校验接口
export function validateTokenCheck() {
  return request({
    url: APP_CONFIG.SSO_API_URL + '/sso/tokenCheck',
    method: 'post',
    data: {}
  })
}

// 2. 获取外部服务访问令牌（用于 SSO/模型等外部接口）
export function getAccessToken() {
  return request({
    url: APP_CONFIG.OAUTH_URL + '/token',
    method: 'get',
    params: {
      grant_type: 'client_credentials',
      scope: 'BROWSER',
      client_id: APP_CONFIG.OAUTH_CLIENT_ID,
      client_secret: APP_CONFIG.OAUTH_CLIENT_SECRET
    },
    _rawResponse: true
  })
}

// 3. 根据 orgId 获取机构信息（返回含 ehrNo 字段的 JSON）
export function getOrgInfoApi(orgId) {
  return request({
    url: `${APP_CONFIG.API_URL}/orginfo/orgid/${orgId}`,
    method: 'get',
    _needsToken: true,
    _rawResponse: true
  })
}

// 4. 获取动态菜单接口（需要 Authorization 令牌）
export function getDynamicMenusApi(userId) {
  return request({
    url: `${APP_CONFIG.API_URL}/model/menulist/userId`,
    method: 'get',
    params: {
      sysId: APP_CONFIG.SYS_ID,
      userId
    },
    _needsToken: true,
    _rawResponse: true
  })
}
