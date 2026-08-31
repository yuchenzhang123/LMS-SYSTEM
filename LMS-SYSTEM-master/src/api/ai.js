import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

const BASE = `${APP_CONFIG.API_URL}/ai`

// LLM 调用较慢（思考模式/推理模型），超时与后端 readTimeout(60s) 对齐
const AI_TIMEOUT = 60000

// 注意：orgCode/ehrNo 必须放在 params（query param），
// 后端 AiQueryInterceptor 用 request.getParameter("orgCode") 读取，读不到 body 里的 JSON 字段
export function aiChatApi({ question, orgCode, ehrNo }) {
  return request({
    url: `${BASE}/chat`,
    method: 'post',
    params: { orgCode, ehrNo },
    data: { question },
    _needsToken: true,
    timeout: AI_TIMEOUT
  })
}

export function dailyBriefingApi({ orgCode, ehrNo, force }) {
  return request({
    url: `${BASE}/briefing`,
    method: 'post',
    // force=true 强制后端绕过缓存重新生成（刷新按钮）；false/undefined 命中缓存
    params: { orgCode, ehrNo, force: force ? true : undefined },
    _needsToken: true,
    timeout: AI_TIMEOUT
  })
}

export function collectionSummaryApi({ loanAccount, orgCode, ehrNo }) {
  return request({
    url: `${BASE}/summary`,
    method: 'post',
    params: { orgCode, ehrNo },
    data: { loanAccount },
    _needsToken: true,
    timeout: AI_TIMEOUT
  })
}
