import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

/**
 * 获取催收账户列表
 * @param {Object} queryParams - 查询参数
 * @param {string} queryParams.customerId - 客户ID
 * @param {string} queryParams.loanAccount - 贷款账户
 * @param {string} queryParams.productCode - 产品代码
 * @param {number} queryParams.overdueDays - 逾期天数
 * @param {Object} queryParams.page - 分页参数
 * @param {number} queryParams.page.currentPage - 当前页码
 * @param {number} queryParams.page.pageSize - 每页大小
 */
export function getAccountStatsApi(params) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/account/stats`,
    method: 'get',
    params,
    _needsToken: true
  })
}

export function getAccountListApi(queryParams) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/account/list`,
    method: 'post',
    data: queryParams,
    _needsToken: true
  })
}

export function getAccountDetailApi(loanAccount) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/account/detail/${loanAccount}`,
    method: 'get',
    _needsToken: true
  })
}

export function sendSmsCollectionApi(smsData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/sms/send`,
    method: 'post',
    data: smsData,
    _needsToken: true
  })
}

export function getCollectionRecordListApi(loanAccount) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/record/list/${loanAccount}`,
    method: 'get',
    _needsToken: true
  })
}

export function addCollectionRecordApi(recordData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/record/add`,
    method: 'post',
    data: recordData,
    headers: { 'Content-Type': 'multipart/form-data' },
    _needsToken: true
  })
}

export function updateCollectionMaterialApi(materialData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/record/update-material`,
    method: 'post',
    data: materialData,
    headers: { 'Content-Type': 'multipart/form-data' },
    _needsToken: true
  })
}

export function getLitigationListApi(loanAccount) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/litigation/list/${loanAccount}`,
    method: 'get',
    _needsToken: true
  })
}

export function getLitigationDetailApi(litigationId) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/litigation/detail/${litigationId}`,
    method: 'get',
    _needsToken: true
  })
}

export function updateLitigationInfoApi(litigationData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/litigation/update`,
    method: 'post',
    data: litigationData,
    _needsToken: true
  })
}

export function uploadFileApi(formData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/material/upload`,
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    _needsToken: true
  })
}

export function downloadMaterialApi(recordId) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/material/download/${recordId}`,
    method: 'get',
    responseType: 'blob',
    _needsToken: true
  })
}

export function getNoticeListApi(params) {
  return request({
    url: `${APP_CONFIG.API_URL}/notice/list`,
    method: 'post',
    data: params,
    _needsToken: true
  })
}

export function markNoticeReadApi(params) {
  return request({
    url: `${APP_CONFIG.API_URL}/notice/mark-read`,
    method: 'post',
    data: params,
    _needsToken: true
  })
}
