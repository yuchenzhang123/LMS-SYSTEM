import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

/**
 * 获取通知列表
 * @param {Object} queryParams - 查询参数
 * @param {number} queryParams.readStatus - 阅读状态 (0-未读, 1-已读, null-全部)
 * @param {Object} queryParams.page - 分页参数
 * @param {number} queryParams.page.currentPage - 当前页码
 * @param {number} queryParams.page.pageSize - 每页大小
 */
export function getNoticeListApi(queryParams) {
  return request({
    url: `${APP_CONFIG.API_URL}/notice/list`,
    method: 'post',
    data: queryParams
  })
}

/**
 * 获取通知详情
 * @param {string} noticeId - 通知ID
 */
export function getNoticeDetailApi(noticeId) {
  return request({
    url: `${APP_CONFIG.API_URL}/notice/detail/${noticeId}`,
    method: 'get'
  })
}

/**
 * 标记通知为已读
 * @param {Array<string>} noticeIds - 通知ID列表
 */
export function markNoticeAsReadApi(noticeIds) {
  return request({
    url: `${APP_CONFIG.API_URL}/notice/mark-read`,
    method: 'post',
    data: { noticeIds }
  })
}

/**
 * 获取未读通知数量
 */
export function getUnreadCountApi() {
  return request({
    url: `${APP_CONFIG.API_URL}/notice/unread-count`,
    method: 'get'
  })
}