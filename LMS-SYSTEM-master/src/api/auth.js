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

// 2. 获取 OAuth2 访问令牌（client_credentials 模式）
// 响应为标准 OAuth2 JSON：{ access_token, token_type, expires_in, ... }
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
    _rawResponse: true  // 绕过内部 code === '0' 检查，直接返回原始响应体
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
// 响应可能为原始数组 [...] 或包装格式 { code:'0', data:[...] }，均使用 _rawResponse 绕过拦截器
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
