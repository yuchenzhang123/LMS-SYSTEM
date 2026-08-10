import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

const BASE = `${APP_CONFIG.API_URL}/ai`

export function aiChatApi({ question, orgCode, ehrNo }) {
  return request({
    url: `${BASE}/chat`,
    method: 'post',
    data: { question, orgCode, ehrNo },
    _needsToken: true,
    timeout: 30000
  })
}

export function dailyBriefingApi({ orgCode, ehrNo }) {
  return request({
    url: `${BASE}/briefing`,
    method: 'post',
    data: { orgCode, ehrNo },
    _needsToken: true
  })
}

export function collectionSummaryApi({ loanAccount, orgCode, ehrNo }) {
  return request({
    url: `${BASE}/summary`,
    method: 'post',
    data: { loanAccount, orgCode, ehrNo },
    _needsToken: true
  })
}
