import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

/**
 * 获取催收账户列表
 * @param {Object} queryParams - 查询参数
 * @param {string} queryParams.customerId - 客户ID
 * @param {string} queryParams.productCode - 产品代码
 * @param {number} queryParams.overdueDays - 逾期天数
 * @param {Object} queryParams.page - 分页参数
 * @param {number} queryParams.page.currentPage - 当前页码
 * @param {number} queryParams.page.pageSize - 每页大小
 */
export function getAccountListApi(queryParams) {
  return request({
    url: `${APP_CONFIG.MENU_API_URL}/collection/account/list`,
    method: 'post',
    data: queryParams
  })
}

/**
 * 获取账户详情
 * @param {string} customerId - 客户ID
 */
export function getAccountDetailApi(customerId) {
  return request({
    url: `${APP_CONFIG.MENU_API_URL}/collection/account/detail/${customerId}`,
    method: 'get'
  })
}

/**
 * 发送短信催收
 * @param {Object} smsData - 短信数据
 * @param {string} smsData.customerId - 客户ID
 * @param {string} smsData.templateId - 模板ID
 * @param {string} smsData.content - 短信内容
 * @param {string} smsData.phone - 手机号
 */
export function sendSmsCollectionApi(smsData) {
  return request({
    url: `${APP_CONFIG.MENU_API_URL}/collection/sms/send`,
    method: 'post',
    data: smsData
  })
}

/**
 * 导出催收材料
 * @param {string} materialId - 材料ID
 */
export function exportMaterialApi(materialId) {
  return request({
    url: `${APP_CONFIG.MENU_API_URL}/collection/material/export/${materialId}`,
    method: 'get',
    responseType: 'blob' // 重要：用于文件下载
  })
}