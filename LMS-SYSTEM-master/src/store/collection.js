import { sendSmsCollectionApi } from '@/api/collection'

const getDefaultListState = () => ({
  activeStatus: 'uncollected',
  queryForm: {
    customerId: '',
    productCode: '',
    overdueDays: undefined
  },
  page: {
    currentPage: 1,
    pageSize: 10
  },
  scrollY: 0
})

const state = {
  listState: getDefaultListState(),
  selectedAccount: null,
  selectedAccountSource: 'list',
  selectedNotice: null,
  creditRecordsByCustomerId: {
    '8800231': [
      {
        id: 'c1001',
        type: '放款',
        amount: '50,000.00',
        occurTime: '2025-08-10 09:00',
        remark: '放款成功'
      },
      {
        id: 'c1002',
        type: '还款',
        amount: '5,000.00',
        occurTime: '2026-02-10 10:20',
        remark: '按期还款'
      },
      {
        id: 'c1003',
        type: '逾期',
        amount: '15,000.00',
        occurTime: '2026-03-01 00:00',
        remark: '当前逾期'
      }
    ]
  },
  collectionRecordsByCustomerId: {
    '8800231': [
      {
        id: 'r1001',
        customerId: '8800231',
        method: 'sms',
        methodText: '短信',
        result: '已发送提醒短信',
        operatorId: '954',
        operatorName: '开发管理员',
        time: '2026-03-10 14:00',
        remark: '模板：到期提醒',
        materialType: '',
        materialName: '',
        materialUrl: ''
      },
      {
        id: 'r1002',
        customerId: '8800231',
        method: 'phone',
        methodText: '电话',
        result: '客户承诺 3 日内处理',
        operatorId: '1001',
        operatorName: '业务员A',
        time: '2026-03-12 09:30',
        remark: '客户反馈月底前还款',
        materialType: 'audio',
        materialName: 'call-20260312-0930.wav',
        materialUrl: 'https://example.com/files/call-20260312-0930.wav'
      },
      {
        id: 'r1003',
        customerId: '8800231',
        method: 'visit',
        methodText: '上门',
        result: '已上门核实客户经营情况',
        operatorId: '1001',
        operatorName: '业务员A',
        time: '2026-03-13 15:10',
        remark: '已采集现场照片',
        materialType: 'image',
        materialName: 'visit-photo-20260313.jpg',
        materialUrl: 'https://example.com/files/visit-photo-20260313.jpg'
      }
    ]
  },
  notices: [
    {
      id: 'n1001',
      title: '客户 8800231 临近逾期提醒',
      level: 'high',
      message: '客户 8800231 距离还款日仅剩 2 天，建议尽快完成电话提醒。',
      customerId: '8800231',
      customerName: '张三',
      productName: '个贷-薪享贷',
      overdueAmount: '15,000.00',
      overdueDays: 45,
      lastCallDate: '2026-03-10 14:00',
      dueDate: '2026-03-17',
      createdAt: '2026-03-15 09:30',
      read: false
    },
    {
      id: 'n1002',
      title: '客户 8800457 今日到期提醒',
      level: 'medium',
      message: '客户 8800457 今日到期，当前仍未入账，请及时跟进。',
      customerId: '8800457',
      customerName: '李四',
      productName: '个贷-消费贷',
      overdueAmount: '8,600.00',
      overdueDays: 12,
      lastCallDate: '2026-03-14 17:20',
      dueDate: '2026-03-15',
      createdAt: '2026-03-15 08:45',
      read: false
    },
    {
      id: 'n1003',
      title: '客户 8800679 逾期预警',
      level: 'low',
      message: '客户 8800679 连续 3 日未接通，建议改为短信+微信双通道提醒。',
      customerId: '8800679',
      customerName: '王五',
      productName: '个贷-经营贷',
      overdueAmount: '26,400.00',
      overdueDays: 5,
      lastCallDate: '2026-03-13 11:05',
      dueDate: '2026-03-18',
      createdAt: '2026-03-15 07:58',
      read: true
    }
  ]
}

const mutations = {
  SET_LIST_STATE: (state, payload) => {
    state.listState = {
      activeStatus: payload.activeStatus,
      queryForm: {
        customerId: payload.queryForm.customerId || '',
        productCode: payload.queryForm.productCode || '',
        overdueDays: payload.queryForm.overdueDays
      },
      page: {
        currentPage: Number(payload.page.currentPage) || 1,
        pageSize: Number(payload.page.pageSize) || 10
      },
      scrollY: Number(payload.scrollY) || 0
    }
  },
  SET_SELECTED_ACCOUNT: (state, payload) => {
    state.selectedAccount = payload ? { ...payload } : null
  },
  SET_SELECTED_ACCOUNT_SOURCE: (state, payload) => {
    state.selectedAccountSource = payload || 'list'
  },
  SET_SELECTED_NOTICE: (state, payload) => {
    state.selectedNotice = payload ? { ...payload } : null
  },
  MARK_NOTICE_READ: (state, noticeId) => {
    state.notices = state.notices.map(item => {
      if (item.id === noticeId) {
        return { ...item, read: true }
      }
      return item
    })
  },
  ADD_COLLECTION_RECORD: (state, payload) => {
    const customerId = payload.customerId
    if (!state.collectionRecordsByCustomerId[customerId]) {
      state.collectionRecordsByCustomerId = {
        ...state.collectionRecordsByCustomerId,
        [customerId]: []
      }
    }
    state.collectionRecordsByCustomerId[customerId] = [
      payload,
      ...state.collectionRecordsByCustomerId[customerId]
    ]
  }
}

const actions = {
  saveListState ({ commit }, payload) {
    commit('SET_LIST_STATE', payload)
  },
  setSelectedAccount ({ commit }, payload) {
    const account = payload && payload.account ? payload.account : payload
    const source = payload && payload.source ? payload.source : 'list'
    commit('SET_SELECTED_ACCOUNT', account)
    commit('SET_SELECTED_ACCOUNT_SOURCE', source)
  },
  setSelectedNotice ({ commit }, payload) {
    commit('SET_SELECTED_NOTICE', payload)
    if (payload && payload.id) {
      commit('MARK_NOTICE_READ', payload.id)
    }
  },
  openNotice ({ dispatch }, payload) {
    if (!payload) {
      return
    }
    dispatch('setSelectedNotice', payload)
    dispatch('setSelectedAccount', {
      source: 'notice',
      account: {
        customerId: payload.customerId,
        customerName: payload.customerName,
        productName: payload.productName,
        overdueAmount: payload.overdueAmount,
        overdueDays: payload.overdueDays,
        lastCallDate: payload.lastCallDate
      }
    })
  },
  async sendSmsCollection ({ commit, rootState }, payload) {
    const operator = rootState.permission && rootState.permission.userInfo ? rootState.permission.userInfo : {}
    const customerId = payload.customerId
    let remoteSuccess = true
    try {
      await sendSmsCollectionApi({
        customerId,
        templateCode: payload.templateCode || 'OVERDUE_REMIND',
        content: payload.content
      })
    } catch (e) {
      remoteSuccess = false
    }
    const record = {
      id: `r${Date.now()}`,
      customerId,
      method: 'sms',
      methodText: '短信',
      result: '短信催收已执行',
      operatorId: operator.userId || '',
      operatorName: operator.userName || '当前用户',
      time: payload.time || new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      remark: payload.content || '',
      materialType: '',
      materialName: '',
      materialUrl: ''
    }
    commit('ADD_COLLECTION_RECORD', record)
    return {
      record,
      remoteSuccess
    }
  },
  addManualCollectionRecord ({ commit, rootState }, payload) {
    const operator = rootState.permission && rootState.permission.userInfo ? rootState.permission.userInfo : {}
    const record = {
      id: `r${Date.now()}`,
      customerId: payload.customerId,
      method: payload.method,
      methodText: payload.methodText,
      result: payload.result,
      operatorId: operator.userId || '',
      operatorName: operator.userName || '当前用户',
      time: payload.time || new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      remark: payload.remark || '',
      materialType: payload.materialType || '',
      materialName: payload.materialName || '',
      materialUrl: payload.materialUrl || ''
    }
    commit('ADD_COLLECTION_RECORD', record)
    return record
  }
}

const getters = {
  unreadNoticeCount: state => state.notices.filter(item => !item.read).length,
  getCreditRecordsByCustomerId: state => customerId => {
    return state.creditRecordsByCustomerId[customerId] || []
  },
  getCollectionRecordsByCustomerId: state => customerId => {
    return state.collectionRecordsByCustomerId[customerId] || []
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
