import request from '@/utils/request'
import { APP_CONFIG } from '@/config'

export function sendSmsCollectionApi(data) {
  return request({
    url: `${APP_CONFIG.MENU_API_URL}/collection/sms/send`,
    method: 'post',
    data
  })
}
