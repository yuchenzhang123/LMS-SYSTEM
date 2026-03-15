<template>
  <div class="account-container">
    <el-card shadow="never" class="filter-card">
      <el-tabs v-model="activeStatus" @tab-click="handleTabChange">
        <el-tab-pane label="未催收" name="uncollected"></el-tab-pane>
        <el-tab-pane label="催收中" name="collecting"></el-tab-pane>
        <el-tab-pane label="已完成" name="completed"></el-tab-pane>
      </el-tabs>

      <el-form :inline="true" :model="queryForm" class="search-form" size="small">
        <el-form-item label="客户号">
          <el-input v-model="queryForm.customerId" placeholder="请输入客户号" clearable></el-input>
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
        <el-table-column prop="customerName" label="客户姓名" width="100"></el-table-column>
        <el-table-column prop="productName" label="产品名称"></el-table-column>
        <el-table-column prop="overdueAmount" label="逾期本金" width="120">
          <template slot-scope="scope">
            <span style="color: #f56c6c; font-weight: bold;">{{ scope.row.overdueAmount }} 元</span>
          </template>
        </el-table-column>
        <el-table-column prop="overdueDays" label="逾期天数" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.overdueDays > 30 ? 'danger' : 'warning'">
              {{ scope.row.overdueDays }} 天
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastCallDate" label="最后催收时间" width="160"></el-table-column>
        <el-table-column label="操作" width="130" align="center" header-align="center">
          <template slot-scope="scope">
            <el-button class="action-enter-btn" size="mini" type="primary" plain @click="goDetail(scope.row)">
              进入详情 >
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
export default {
  name: 'AccountList',
  data () {
    return {
      activeStatus: 'uncollected',
      loading: false,
      queryForm: {
        customerId: '',
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
      this.loading = true
      // 模拟请求延迟
      setTimeout(() => {
        this.tableData = [
          {
            customerId: '8800231',
            customerName: '张三',
            productName: '个贷-薪享贷',
            overdueAmount: '15,000.00',
            overdueDays: 45,
            lastCallDate: '2026-03-10 14:00'
          }
        ]
        this.page.total = 1
        this.loading = false
        if (this.shouldRestoreScroll) {
          this.$nextTick(() => {
            window.scrollTo(0, this.listScrollY)
            this.shouldRestoreScroll = false
          })
        }
      }, 500)
    },
    resetQuery () {
      this.queryForm = {
        customerId: '',
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
      this.$store.dispatch('collection/setSelectedAccount', {
        source: 'list',
        account: row
      })
      this.$router.push({
        path: '/collection/account-detail'
      })
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
