import {
  sendSmsCollectionApi,
  getAccountListApi,
  getAccountDetailApi,
  getCollectionRecordListApi,
  addCollectionRecordApi,
  updateCollectionMaterialApi,
  getLitigationListApi,
  getLitigationDetailApi,
  updateLitigationInfoApi,
  getNoticeListApi,
  markNoticeReadApi,
  uploadFileApi
} from '@/api/collection'

const getDefaultListState = () => ({
  activeStatus: 'uncollected',
  queryForm: {
    customerId: '',
    loanAccount: '',
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
  // 以贷款账户为主键存储数据（从后端获取）
  litigationListByLoanAccount: {}, // 改为列表存储
  collectionRecordsByLoanAccount: {},
  // 通知列表（从后端获取）
  notices: [],
  noticesTotal: 0,
  noticesUnreadCount: 0
}

const mutations = {
  SET_LIST_STATE: (state, payload) => {
    state.listState = {
      activeStatus: payload.activeStatus,
      queryForm: {
        customerId: payload.queryForm.customerId || '',
        loanAccount: payload.queryForm.loanAccount || '',
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
  SET_NOTICES: (state, payload) => {
    state.notices = payload.records || []
    state.noticesTotal = payload.total || 0
    state.noticesUnreadCount = payload.unreadCount || 0
  },
  MARK_NOTICE_READ: (state, noticeId) => {
    state.notices = state.notices.map(item => {
      if (item.id === noticeId) {
        return { ...item, read: true }
      }
      return item
    })
    if (state.noticesUnreadCount > 0) {
      state.noticesUnreadCount--
    }
  },
  ADD_COLLECTION_RECORD: (state, payload) => {
    const loanAccount = payload.loanAccount
    if (!state.collectionRecordsByLoanAccount[loanAccount]) {
      state.collectionRecordsByLoanAccount = {
        ...state.collectionRecordsByLoanAccount,
        [loanAccount]: []
      }
    }
    state.collectionRecordsByLoanAccount[loanAccount] = [
      payload,
      ...state.collectionRecordsByLoanAccount[loanAccount]
    ]
  },
  SET_LITIGATION_LIST_BY_LOAN_ACCOUNT: (state, payload) => {
    state.litigationListByLoanAccount = {
      ...state.litigationListByLoanAccount,
      [payload.loanAccount]: payload.records || []
    }
  },
  UPSERT_LITIGATION_INFO: (state, payload) => {
    const loanAccount = payload.loanAccount
    const litigationId = payload.litigationId
    const list = state.litigationListByLoanAccount[loanAccount] || []
    const existingIndex = list.findIndex(item => item.litigationId === litigationId)
    if (existingIndex >= 0) {
      // 更新已有记录
      list.splice(existingIndex, 1, payload)
    } else {
      // 新增记录，放在开头
      list.unshift(payload)
    }
    state.litigationListByLoanAccount = {
      ...state.litigationListByLoanAccount,
      [loanAccount]: [...list]
    }
  },
  SET_COLLECTION_RECORDS_BY_LOAN_ACCOUNT: (state, payload) => {
    state.collectionRecordsByLoanAccount = {
      ...state.collectionRecordsByLoanAccount,
      [payload.loanAccount]: payload.records || []
    }
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
        loanAccount: payload.loanAccount,
        customerId: payload.customerId,
        customerName: payload.customerName,
        productCode: payload.productCode,
        overdueDays: payload.overdueDays
      }
    })
  },
  // 获取通知列表
  async fetchNoticeList ({ commit }, payload) {
    const page = payload && payload.page ? payload.page : { currentPage: 1, pageSize: 10 }
    const readStatus = payload && typeof payload.readStatus === 'number' ? payload.readStatus : null
    const response = await getNoticeListApi({
      page: {
        currentPage: page.currentPage || 1,
        pageSize: page.pageSize || 10
      },
      readStatus
    })
    // response 格式: { code: '0', data: { records: [...], total: 3, unreadCount: 1 }, message: '成功' }
    const data = response && response.data ? response.data : (response || {})
    commit('SET_NOTICES', {
      records: data.records || [],
      total: data.total || 0,
      unreadCount: data.unreadCount || 0
    })
    return data
  },
  // 标记通知已读
  async markNoticeRead ({ commit }, noticeId) {
    await markNoticeReadApi({ noticeIds: [noticeId] })
    commit('MARK_NOTICE_READ', noticeId)
  },
  async fetchAccountList (_, payload) {
    const queryForm = payload && payload.queryForm ? payload.queryForm : {}
    const page = payload && payload.page ? payload.page : { currentPage: 1, pageSize: 10 }
    const response = await getAccountListApi({
      customerId: queryForm.customerId || '',
      loanAccount: queryForm.loanAccount || '',
      productCode: queryForm.productCode || '',
      overdueDays: typeof queryForm.overdueDays === 'number' ? queryForm.overdueDays : null,
      status: queryForm.status || '', // 添加状态筛选
      page: {
        currentPage: page.currentPage || 1,
        pageSize: page.pageSize || 10
      }
    })
    // response 格式: { code: '0', data: { records: [...], total: 3, ... }, message: '成功' }
    const data = response && response.data ? response.data : (response || {})
    return {
      records: data.records || [],
      total: Number(data.total || 0),
      current: Number(data.current || page.currentPage || 1),
      size: Number(data.size || page.pageSize || 10)
    }
  },
  async loadAccountDetailData ({ commit }, loanAccount) {
    if (!loanAccount) {
      return null
    }
    const [accountDetailRes, collectionRecordsRes, litigationListRes] = await Promise.all([
      getAccountDetailApi(loanAccount),
      getCollectionRecordListApi(loanAccount),
      getLitigationListApi(loanAccount)
    ])
    
    // 提取实际数据 (response.data)
    const accountDetail = accountDetailRes && accountDetailRes.data ? accountDetailRes.data : accountDetailRes
    const collectionRecords = collectionRecordsRes && collectionRecordsRes.data ? collectionRecordsRes.data : collectionRecordsRes
    const litigationList = litigationListRes && litigationListRes.data ? litigationListRes.data : litigationListRes
    
    // 更新选中账户的完整信息
    commit('SET_SELECTED_ACCOUNT', {
      loanAccount: loanAccount,
      customerId: accountDetail && accountDetail.customerId ? accountDetail.customerId : '--',
      customerName: accountDetail && accountDetail.customerName ? accountDetail.customerName : '--',
      orgName: accountDetail && accountDetail.orgName ? accountDetail.orgName : '--',
      phone: accountDetail && accountDetail.phone ? accountDetail.phone : '',
      productCode: accountDetail && accountDetail.productCode ? accountDetail.productCode : '--',
      loanDate: accountDetail && accountDetail.loanDate ? accountDetail.loanDate : '--',
      loanTerm: accountDetail && accountDetail.loanTerm ? accountDetail.loanTerm : 0,
      overdueDays: Number(accountDetail && accountDetail.overdueDays ? accountDetail.overdueDays : 0),
      contractAmount: accountDetail && accountDetail.contractAmount ? accountDetail.contractAmount : '--',
      loanBalance: accountDetail && accountDetail.loanBalance ? accountDetail.loanBalance : '--',
      unexpiredPrincipal: accountDetail && accountDetail.unexpiredPrincipal ? accountDetail.unexpiredPrincipal : '--',
      overduePrincipal: accountDetail && accountDetail.overduePrincipal ? accountDetail.overduePrincipal : '--',
      overdueInterest: accountDetail && accountDetail.overdueInterest ? accountDetail.overdueInterest : '--',
      overduePenalty: accountDetail && accountDetail.overduePenalty ? accountDetail.overduePenalty : '--',
      totalOverdueAmount: accountDetail && accountDetail.totalOverdueAmount ? accountDetail.totalOverdueAmount : '--'
    })
    
    commit('SET_COLLECTION_RECORDS_BY_LOAN_ACCOUNT', {
      loanAccount,
      records: collectionRecords || []
    })
    commit('SET_LITIGATION_LIST_BY_LOAN_ACCOUNT', {
      loanAccount,
      records: litigationList || []
    })
    return {
      accountDetail,
      collectionRecords,
      litigationList
    }
  },
  async sendSmsCollection ({ commit, rootState }, payload) {
    const operator = rootState.permission && rootState.permission.userInfo ? rootState.permission.userInfo : {}
    const loanAccount = payload.loanAccount
    const customerId = payload.customerId
    let remoteSuccess = true
    try {
      await sendSmsCollectionApi({
        loanAccount,
        customerId,
        templateCode: payload.templateCode || 'OVERDUE_REMIND',
        content: payload.content,
        phone: payload.phone || '',
        operatorId: operator.userId || '',
        operatorName: operator.userName || '当前用户'
      })
    } catch (e) {
      remoteSuccess = false
    }
    const record = {
      id: `r${Date.now()}`,
      loanAccount,
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
  async addManualCollectionRecord ({ commit, rootState }, payload) {
    const operator = rootState.permission && rootState.permission.userInfo ? rootState.permission.userInfo : {}

    // 构建FormData（后端现在接收multipart/form-data格式）
    const formData = new FormData()
    formData.append('loanAccount', payload.loanAccount)
    formData.append('customerId', payload.customerId)
    formData.append('targetType', payload.targetType || '')
    formData.append('targetName', payload.targetName || '')
    formData.append('actualCollectionTime', payload.actualCollectionTime || '')
    formData.append('method', payload.method)
    formData.append('methodText', payload.methodText)
    formData.append('result', payload.result)
    formData.append('operatorId', operator.userId || '')
    formData.append('operatorName', operator.userName || '当前用户')
    formData.append('time', payload.time || new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'))
    formData.append('remark', payload.remark || '')
    formData.append('materialType', payload.materialType || '')
    formData.append('materialName', payload.materialName || '')
    // 注意：文件已经在account-detail.vue中处理过了，materialUrl已经是真实的URL了

    // 使用FormData调用API
    const response = await addCollectionRecordApi(formData)
    const record = response.data

    commit('ADD_COLLECTION_RECORD', record)
    return record
  },
  async updateCollectionMaterial ({ commit, rootState }, payload) {
    // 构建FormData（后端现在接收multipart/form-data格式）
    const formData = new FormData()
    formData.append('recordId', payload.recordId)
    formData.append('materialType', payload.materialType)
    formData.append('materialName', payload.materialName)
    if (payload.rawFile) {
      formData.append('file', payload.rawFile)
    }

    const updatedRecord = await updateCollectionMaterialApi(formData)
    commit('UPDATE_COLLECTION_RECORD', { ...updatedRecord, loanAccount: payload.loanAccount })
    return updatedRecord
  },
  async updateLitigationProgress ({ commit, rootState }, payload) {
    const operator = rootState.permission && rootState.permission.userInfo ? rootState.permission.userInfo : {}
    const loanAccount = payload.loanAccount
    const customerId = payload.customerId
    const response = await updateLitigationInfoApi({
      litigationId: payload.litigationId || '', // 支持诉讼ID
      loanAccount,
      customerId,
      statusCode: payload.statusCode,
      statusText: payload.statusText,
      inLitigation: typeof payload.inLitigation === 'boolean' ? payload.inLitigation : true,
      submitToLawFirmDate: payload.submitToLawFirmDate || '',
      submitToCourtDate: payload.submitToCourtDate || '',
      filingCaseNo: payload.filingCaseNo || '',
      isHearing: typeof payload.isHearing === 'boolean' ? payload.isHearing : false,
      hearingDate: payload.hearingDate || '',
      judgmentDate: payload.judgmentDate || '',
      executionApplyToCourtDate: payload.executionApplyToCourtDate || '',
      executionFilingDate: payload.executionFilingDate || '',
      executionCaseNo: payload.executionCaseNo || '',
      auctionStatus: payload.auctionStatus || '',
      litigationFee: payload.litigationFee || '',
      litigationFeePaidByCustomer: !!payload.litigationFeePaidByCustomer,
      preservationFee: payload.preservationFee || '',
      preservationFeePaidByCustomer: !!payload.preservationFeePaidByCustomer,
      appraisalFee: payload.appraisalFee || '',
      litigationPreservationPaidAt: payload.litigationPreservationPaidAt || '',
      litigationPreservationWriteOffAt: payload.litigationPreservationWriteOffAt || '',
      lawyerFee: payload.lawyerFee || '',
      lawyerFeePaidByCustomer: !!payload.lawyerFeePaidByCustomer,
      courtName: payload.courtName || '',
      lawFirm: payload.lawFirm || '',
      remark: payload.remark || '',
      operatorId: operator.userId || '',
      operatorName: operator.userName || '当前用户'
    })
    const litigationInfo = response && response.litigationInfo ? response.litigationInfo : null
    const record = response && response.record ? response.record : null
    if (litigationInfo) {
      commit('UPSERT_LITIGATION_INFO', litigationInfo)
    }
    if (!record) {
      return {
        litigationInfo,
        record: null
      }
    }
    commit('ADD_COLLECTION_RECORD', record)
    return {
      litigationInfo,
      record
    }
  }
}

const getters = {
  unreadNoticeCount: state => state.noticesUnreadCount,
  getCollectionRecordsByLoanAccount: state => loanAccount => {
    return state.collectionRecordsByLoanAccount[loanAccount] || []
  },
  getLitigationListByLoanAccount: state => loanAccount => {
    return state.litigationListByLoanAccount[loanAccount] || []
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}
