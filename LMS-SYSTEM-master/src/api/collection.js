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
export function getAccountListApi(queryParams) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/account/list`,
    method: 'post',
    data: queryParams
  })
}

/**
 * 获取账户详情
 * @param {string} loanAccount - 贷款账户
 */
export function getAccountDetailApi(loanAccount) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/account/detail/${loanAccount}`,
    method: 'get'
  })
}

/**
 * 发送短信催收
 * @param {Object} smsData - 短信数据
 * @param {string} smsData.loanAccount - 贷款账户
 * @param {string} smsData.customerId - 客户ID
 * @param {string} smsData.templateId - 模板ID
 * @param {string} smsData.content - 短信内容
 * @param {string} smsData.phone - 手机号
 */
export function sendSmsCollectionApi(smsData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/sms/send`,
    method: 'post',
    data: smsData
  })
}

/**
 * 获取催收记录列表
 * @param {string} loanAccount - 贷款账户
 */
export function getCollectionRecordListApi(loanAccount) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/record/list/${loanAccount}`,
    method: 'get'
  })
}

/**
 * 新增催收记录
 * @param {FormData} recordData - 催收记录数据（FormData格式）
 */
export function addCollectionRecordApi(recordData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/record/add`,
    method: 'post',
    data: recordData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 更新催收记录材料（补交/重交）
 * @param {FormData} materialData - 材料数据（FormData格式）
 */
export function updateCollectionMaterialApi(materialData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/record/update-material`,
    method: 'post',
    data: materialData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取诉讼信息列表
 * @param {string} loanAccount - 贷款账户
 */
export function getLitigationListApi(loanAccount) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/litigation/list/${loanAccount}`,
    method: 'get'
  })
}

/**
 * 获取诉讼详情
 * @param {string} litigationId - 诉讼ID
 */
export function getLitigationDetailApi(litigationId) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/litigation/detail/${litigationId}`,
    method: 'get'
  })
}

/**
 * 更新诉讼信息
 * @param {Object} litigationData - 诉讼数据
 * @param {string} litigationData.litigationId - 诉讼ID（新增时为空，更新时必填）
 * @param {string} litigationData.loanAccount - 贷款账户
 * @param {string} litigationData.customerId - 客户ID
 */
export function updateLitigationInfoApi(litigationData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/litigation/update`,
    method: 'post',
    data: litigationData
  })
}

/**
 * 上传文件
 * @param {FormData} formData - 包含文件的表单数据
 */
export function uploadFileApi(formData) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/material/upload`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 下载催收材料
 * @param {string} recordId - 记录ID
 */
export function downloadMaterialApi(recordId) {
  return request({
    url: `${APP_CONFIG.API_URL}/collection/material/download/${recordId}`,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 获取通知列表
 * @param {Object} params - 查询参数
 * @param {Object} params.page - 分页参数
 * @param {number} params.page.currentPage - 当前页码
 * @param {number} params.page.pageSize - 每页大小
 * @param {number} params.readStatus - 读取状态 0-未读 1-已读
 */
export function getNoticeListApi(params) {
  return request({
    url: `${APP_CONFIG.API_URL}/notice/list`,
    method: 'post',
    data: params
  })
}

/**
 * 标记通知已读
 * @param {Object} params - 参数
 * @param {string[]} params.noticeIds - 通知ID列表
 */
export function markNoticeReadApi(params) {
  return request({
    url: `${APP_CONFIG.API_URL}/notice/mark-read`,
    method: 'post',
    data: params
  })
}
