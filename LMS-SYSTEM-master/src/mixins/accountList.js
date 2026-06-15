/**
 * 账户列表页公共逻辑（collection/account-list、admin/account-list 共用）
 *
 * 依赖 keep-alive（layout/index.vue 中配置）：
 * - 首次进入：正常 mount → fetchData
 * - 从详情返回：DOM 原样呈现，滚动位置自然保留
 */
export default {
  methods: {
    // ---- 状态标签 ----
    getStatusTagType (status) {
      return { uncollected: 'info', collecting: 'warning', completed: 'success' }[status] || 'info'
    },
    getStatusText (status) {
      return { uncollected: '未催收', collecting: '催收中', completed: '已还款' }[status] || status
    },

    // ---- 分页 ----
    handleTabChange () {
      this.page.currentPage = 1
      this.fetchData()
    },
    handleSizeChange (val) {
      this.page.pageSize = val
      this.fetchData()
    },
    handleCurrentChange (val) {
      this.page.currentPage = val
      this.fetchData()
    },

    // ---- 跳转详情 ----
    goDetail (row) {
      this.syncListStateToStore()
      this.$store.dispatch('collection/setSelectedAccount', {
        source: this.$route.path,
        account: {
          loanAccount: row.loanAccount,
          customerId: row.customerId,
          customerName: row.customerName,
          productCode: row.productCode,
          overdueDays: row.overdueDays,
          status: row.status
        }
      })
      this.$router.push({
        path: '/collection/account-detail',
        query: { loanAccount: row.loanAccount }
      })
    },

    // ---- fetch 完成 ----
    afterFetch () {
      this.loading = false
    }
  }
}
