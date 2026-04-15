<template>
  <div class="account-container">
    <el-card shadow="never" class="filter-card">
      <el-tabs v-model="activeStatus" @tab-click="handleTabChange">
        <el-tab-pane label="未催收" name="uncollected"></el-tab-pane>
        <el-tab-pane label="催收中" name="collecting"></el-tab-pane>
        <el-tab-pane label="已还款" name="completed"></el-tab-pane>
      </el-tabs>

      <el-form :inline="true" :model="queryForm" class="search-form" size="small">
        <el-form-item label="客户号">
          <el-input v-model="queryForm.customerId" placeholder="请输入客户号" clearable></el-input>
        </el-form-item>
        <el-form-item label="贷款账户">
          <el-input v-model="queryForm.loanAccount" placeholder="请输入贷款账户" clearable></el-input>
        </el-form-item>
        <el-form-item label="产品码">
          <el-input v-model="queryForm.productCode" placeholder="请输入产品码" clearable></el-input>
        </el-form-item>
        <el-form-item label="逾期天数">
          <el-input-number v-model="queryForm.overdueDays" :min="0" placeholder="天数"></el-input-number>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="fetchData">查询</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="customerId" label="客户号" width="120"></el-table-column>
        <el-table-column prop="customerName" label="客户名" width="100"></el-table-column>
        <el-table-column prop="loanAccount" label="贷款账户" min-width="160"></el-table-column>
        <el-table-column prop="productCode" label="产品码" width="100"></el-table-column>
        <el-table-column prop="overdueDays" label="逾期天数" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.overdueDays > 30 ? 'danger' : 'warning'">
              {{ scope.row.overdueDays }} 天
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button class="action-enter-btn" size="mini" type="primary" plain @click="goDetail(scope.row)">
              进入详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="page.currentPage"
        :page-sizes="[10, 20, 50]"
        :page-size="page.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="page.total"
        style="margin-top: 20px; text-align: right;"
      >
      </el-pagination>
    </el-card>
  </div>
</template>

<script>
import { Message } from 'element-ui'

export default {
  name: 'AccountList',
  data () {
    return {
      activeStatus: 'uncollected',
      loading: false,
      queryForm: {
        customerId: '',
        loanAccount: '',
        productCode: '',
        overdueDays: undefined
      },
      listScrollY: 0,
      shouldRestoreScroll: false,
      restoringStoreState: false,
      scrollSyncTimer: null,
      tableData: [],
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      }
    }
  },
  created () {
    this.restoreStateFromStore()
    this.fetchData()
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
  watch: {
    activeStatus () {
      this.syncListStateToStore()
    },
    'queryForm.customerId' () {
      this.syncListStateToStore()
    },
    'queryForm.loanAccount' () {
      this.syncListStateToStore()
    },
    'queryForm.productCode' () {
      this.syncListStateToStore()
    },
    'queryForm.overdueDays' () {
      this.syncListStateToStore()
    },
    'page.currentPage' () {
      this.syncListStateToStore()
    },
    'page.pageSize' () {
      this.syncListStateToStore()
    }
  },
  methods: {
    restoreStateFromStore () {
      this.restoringStoreState = true
      const savedState = this.$store.state.collection && this.$store.state.collection.listState
      if (savedState) {
        this.activeStatus = savedState.activeStatus || 'uncollected'
        this.queryForm = {
          customerId: savedState.queryForm && savedState.queryForm.customerId ? savedState.queryForm.customerId : '',
          loanAccount: savedState.queryForm && savedState.queryForm.loanAccount ? savedState.queryForm.loanAccount : '',
          productCode: savedState.queryForm && savedState.queryForm.productCode ? savedState.queryForm.productCode : '',
          overdueDays: savedState.queryForm ? savedState.queryForm.overdueDays : undefined
        }
        this.page.currentPage = savedState.page && savedState.page.currentPage ? Number(savedState.page.currentPage) : 1
        this.page.pageSize = savedState.page && savedState.page.pageSize ? Number(savedState.page.pageSize) : 10
        this.listScrollY = Number(savedState.scrollY || 0)
        this.shouldRestoreScroll = this.listScrollY > 0
      }
      this.$nextTick(() => {
        this.restoringStoreState = false
        this.syncListStateToStore()
      })
    },
    syncListStateToStore () {
      if (this.restoringStoreState) {
        return
      }
      this.$store.dispatch('collection/saveListState', {
        activeStatus: this.activeStatus,
        queryForm: {
          customerId: this.queryForm.customerId || '',
          loanAccount: this.queryForm.loanAccount || '',
          productCode: this.queryForm.productCode || '',
          overdueDays: this.queryForm.overdueDays
        },
        page: {
          currentPage: this.page.currentPage,
          pageSize: this.page.pageSize
        },
        scrollY: window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0
      })
    },
    handleScroll () {
      if (this.scrollSyncTimer) {
        clearTimeout(this.scrollSyncTimer)
      }
      this.scrollSyncTimer = setTimeout(() => {
        this.syncListStateToStore()
      }, 150)
    },
    handleTabChange () {
      this.page.currentPage = 1
      this.fetchData()
    },
    async fetchData () {
      const userRole = this.$store.state.permission.userRole
      const orgCode = this.$store.state.permission.orgCode
      // 业务员必须有机构号才允许查询，否则拒绝返回任何数据
      if (userRole === 'staff' && !orgCode) {
        this.tableData = []
        this.page.total = 0
        return
      }
      this.loading = true
      try {
        const data = await this.$store.dispatch('collection/fetchAccountList', {
          queryForm: {
            ...this.queryForm,
            status: this.activeStatus,
            // 业务员强制用自己的机构号过滤，不可被覆盖
            branchCode: userRole === 'staff' ? orgCode : undefined
          },
          page: this.page
        })
        this.tableData = data.records || []
        this.page.total = Number(data.total || 0)
        this.page.currentPage = Number(data.current || this.page.currentPage)
        this.page.pageSize = Number(data.size || this.page.pageSize)
      } catch (e) {
        this.tableData = []
        this.page.total = 0
        // 错误已在 request.js 中统一处理显示
        console.warn('查询账户列表失败:', e.message)
      } finally {
        this.loading = false
        if (this.shouldRestoreScroll) {
          this.$nextTick(() => {
            window.scrollTo(0, this.listScrollY)
            this.shouldRestoreScroll = false
          })
        }
      }
    },
    resetQuery () {
      this.queryForm = {
        customerId: '',
        loanAccount: '',
        productCode: '',
        overdueDays: undefined
      }
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
    goDetail (row) {
      this.syncListStateToStore()
      // 以贷款账户作为主键传递
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
    getStatusTagType (status) {
      const typeMap = {
        'uncollected': 'info',
        'collecting': 'warning',
        'completed': 'success'
      }
      return typeMap[status] || 'info'
    },
    getStatusText (status) {
      const textMap = {
        'uncollected': '未催收',
        'collecting': '催收中',
        'completed': '已还款'
      }
      return textMap[status] || status
    }
  }
}
</script>

<style scoped>
.account-container {
  padding: 10px;
}
.filter-card {
  margin-bottom: 15px;
}
.search-form {
  margin-top: 20px;
  border-top: 1px solid #f0f0f0;
  padding-top: 20px;
}
.table-card {
  min-height: 500px;
}
.action-enter-btn {
  min-width: 86px;
  padding: 6px 10px;
  border-color: #d9ecff;
  background-color: #f5f9ff;
  color: #409EFF;
}
.action-enter-btn:hover,
.action-enter-btn:focus {
  background-color: #ecf5ff;
  border-color: #b3d8ff;
  color: #2d8cf0;
}
</style>
