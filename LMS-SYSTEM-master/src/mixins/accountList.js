/**
 * 账户列表页公共逻辑（collection/account-list、admin/account-list 共用）
 */
export default {
  data () {
    return {
      listScrollY: 0,
      shouldRestoreScroll: false,
      restoringStoreState: false,
      scrollSyncTimer: null
    }
  },
  mounted () {
    window.addEventListener('scroll', this.handleScroll, { passive: true })
  },
  beforeDestroy () {
    window.removeEventListener('scroll', this.handleScroll)
    if (this.scrollSyncTimer) {
      clearTimeout(this.scrollSyncTimer)
      this.scrollSyncTimer = null
    }
  },
  methods: {
    // ---- 状态标签 ----
    getStatusTagType (status) {
      return { uncollected: 'info', collecting: 'warning', completed: 'success' }[status] || 'info'
    },
    getStatusText (status) {
      return { uncollected: '未催收', collecting: '催收中', completed: '已还款' }[status] || status
    },

    // ---- 滚动位置持久化 ----
    handleScroll () {
      if (this.scrollSyncTimer) clearTimeout(this.scrollSyncTimer)
      this.scrollSyncTimer = setTimeout(() => {
        this.syncListStateToStore()
      }, 150)
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
        source: 'list',
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

    // ---- fetch 后恢复滚动 ----
    afterFetch () {
      if (this.shouldRestoreScroll) {
        this.$nextTick(() => {
          window.scrollTo(0, this.listScrollY)
          this.shouldRestoreScroll = false
        })
      }
    }
  }
}
