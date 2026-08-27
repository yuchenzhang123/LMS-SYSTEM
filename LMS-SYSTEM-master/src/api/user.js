import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

const BASE = `${APP_CONFIG.API_URL}/user`

export function loginLogApi({ ehrNo, userName, orgCode }) {
  return request({
    url: `${BASE}/login-log`,
    method: 'post',
    data: { ehrNo, userName, orgCode },
    _needsToken: true
  })
}

export function getUserStatsApi({ orgCode, ehrNo } = {}) {
  return request({
    url: `${BASE}/stats`,
    method: 'get',
    params: { orgCode, ehrNo },
    _needsToken: true
  })
}

export function getUserListApi({ orgCode, ehrNo, startDate, endDate, page, size, sortBy } = {}) {
  return request({
    url: `${BASE}/list`,
    method: 'get',
    params: { orgCode, ehrNo, startDate, endDate, page, size, sortBy },
    _needsToken: true
  })
}
