/**
 * 账户列表页公共逻辑（collection/account-list、admin/account-list 共用）
 *
 * 依赖 keep-alive（layout/index.vue 中配置，由 route.meta.keepAlive 控制）：
 * - 首次进入：正常 mount → fetchData
 * - 从详情返回：activated → 恢复scrollTop → fetchData 刷新数据 → afterFetch 再恢复
 *
 * 滚动容器：el-main（.app-main），固定高度 + overflow:auto，不是 window
 */
export default {
  data () {
    return {
      savedScrollTop: 0
    }
  },
  deactivated () {
    // 离开组件时保存滚动容器的 scrollTop
    const scroller = this.getScrollContainer()
    if (scroller) {
      this.savedScrollTop = scroller.scrollTop
    }
  },
  activated () {
    // 切回时先恢复滚动位置（DOM 已缓存，但 scrollTop 可能被重置），再刷新数据
    const scroller = this.getScrollContainer()
    if (scroller && this.savedScrollTop > 0) {
      scroller.scrollTop = this.savedScrollTop
    }
    this.fetchData()
  },
  methods: {
    // ---- 滚动容器 ----
    getScrollContainer () {
      return document.querySelector('.app-main')
    },

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
      if (this.savedScrollTop > 0) {
        this.$nextTick(() => {
          const scroller = this.getScrollContainer()
          if (scroller) {
            scroller.scrollTop = this.savedScrollTop
          }
        })
      }
      this.loading = false
    }
  }
}
