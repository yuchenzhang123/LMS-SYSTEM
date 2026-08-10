<template>
  <div class="account-container">
    <el-card shadow="never" class="filter-card">
      <el-tabs v-model="activeStatus" @tab-click="handleTabChange">
        <el-tab-pane label="未催收" name="uncollected"></el-tab-pane>
        <el-tab-pane label="催收中" name="collecting"></el-tab-pane>
        <el-tab-pane label="已还款" name="completed"></el-tab-pane>
      </el-tabs>

      <div class="toolbar-row">
        <el-button size="small" icon="el-icon-download" @click="exportDialogVisible = true">导出</el-button>
        <el-button size="small" icon="el-icon-folder-opened" @click="downloadCenterVisible = true">下载中心</el-button>
      </div>
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
      <el-radio-group v-model="sortMode" size="small" @change="fetchData" style="margin-bottom:12px">
        <el-radio-button label="time">最新逾期</el-radio-button>
        <el-radio-button label="priority">智能排序</el-radio-button>
        <el-radio-button label="amount">金额最高</el-radio-button>
      </el-radio-group>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="customerId" label="客户号" min-width="80" align="left" header-align="left"></el-table-column>
        <el-table-column prop="customerName" label="客户名" min-width="80" align="left" header-align="left"></el-table-column>
        <el-table-column prop="loanAccount" label="贷款账户" min-width="140" align="left" header-align="left"></el-table-column>
        <el-table-column prop="branchName" label="归属机构" min-width="120" align="left" header-align="left"></el-table-column>
        <el-table-column prop="productCode" label="产品码" min-width="80" align="left" header-align="left"></el-table-column>
        <el-table-column prop="overdueDays" label="逾期天数" min-width="90" align="left" header-align="left">
          <template slot-scope="scope">
            <el-tag :type="scope.row.overdueDays > 30 ? 'danger' : 'warning'">{{ scope.row.overdueDays }} 天</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loanBalance" label="贷款余额" min-width="110" align="left" header-align="left">
          <template slot-scope="scope"><span>¥ {{ scope.row.loanBalance }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="80" align="left" header-align="left">
          <template slot-scope="scope">
            <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button class="action-enter-btn" size="mini" type="primary" plain @click="goDetail(scope.row)">进入详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        @size-change="handleSizeChange" @current-change="handleCurrentChange"
        :current-page="page.currentPage" :page-sizes="[10, 20, 50, 100]"
        :page-size="page.pageSize" layout="total, sizes, prev, pager, next, jumper"
        :total="page.total" style="margin-top: 20px; text-align: right;">
      </el-pagination>
    </el-card>

    <export-dialog :visible.sync="exportDialogVisible" />
    <download-center :visible.sync="downloadCenterVisible" />
  </div>
</template>

<script>
import accountListMixin from '@/mixins/accountList'
import ExportDialog from '@/components/collection/ExportDialog'
import DownloadCenter from '@/components/collection/DownloadCenter'

export default {
  name: 'AccountList',
  components: { ExportDialog, DownloadCenter },
  mixins: [accountListMixin],
  data () {
    let saved = { page: {} }
    try {
      const raw = sessionStorage.getItem('STAFF_LIST_STATE')
      if (raw) saved = JSON.parse(raw)
    } catch (e) { /* ignore */ }
    return {
      activeStatus: saved.activeStatus || 'uncollected',
      loading: false,
      queryForm: {
        customerId: (saved.queryForm && saved.queryForm.customerId) || '',
        loanAccount: (saved.queryForm && saved.queryForm.loanAccount) || '',
        productCode: (saved.queryForm && saved.queryForm.productCode) || '',
        overdueDays: (saved.queryForm && saved.queryForm.overdueDays) || undefined
      },
      tableData: [],
      page: {
        currentPage: Number((saved.page && saved.page.currentPage)) || 1,
        pageSize: Number((saved.page && saved.page.pageSize)) || 10,
        total: 0
      },
      sortMode: 'time',
      syncTimer: null,
      exportDialogVisible: false,
      downloadCenterVisible: false
    }
  },
  created () {
    this.fetchData()
  },
  watch: {
    activeStatus: 'scheduleSync',
    'queryForm.customerId': 'scheduleSync',
    'queryForm.loanAccount': 'scheduleSync',
    'queryForm.productCode': 'scheduleSync',
    'queryForm.overdueDays': 'scheduleSync',
    'page.currentPage': 'scheduleSync',
    'page.pageSize': 'scheduleSync'
  },
  methods: {
    scheduleSync () {
      clearTimeout(this.syncTimer)
      this.syncTimer = setTimeout(() => this.syncListStateToStore(), 150)
    },
    syncListStateToStore () {
      try {
        sessionStorage.setItem('STAFF_LIST_STATE', JSON.stringify({
          activeStatus: this.activeStatus,
          queryForm: {
            customerId: this.queryForm.customerId || '',
            loanAccount: this.queryForm.loanAccount || '',
            productCode: this.queryForm.productCode || '',
            overdueDays: this.queryForm.overdueDays
          },
          page: { currentPage: this.page.currentPage, pageSize: this.page.pageSize }
        }))
      } catch (e) { /* ignore */ }
    },
    async fetchData () {
      const userRole = this.$store.state.permission.userRole
      const orgCode = this.$store.state.permission.orgCode
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
            branchCode: userRole === 'staff' ? orgCode : undefined,
            sortBy: this.sortMode
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
      } finally {
        this.afterFetch()
      }
    },
    resetQuery () {
      this.queryForm = { customerId: '', loanAccount: '', productCode: '', overdueDays: undefined }
      this.fetchData()
    }
  }
}
</script>

<style scoped>
.account-container { padding: 10px; }
.filter-card { margin-bottom: 15px; }
.toolbar-row { display: flex; justify-content: flex-end; margin-bottom: 4px; }
.search-form { border-top: 1px solid #f0f0f0; padding-top: 20px; }
.table-card { min-height: 500px; }
.action-enter-btn {
  min-width: 86px; padding: 6px 10px;
  border-color: #d9ecff; background-color: #f5f9ff; color: #409EFF;
}
.action-enter-btn:hover, .action-enter-btn:focus {
  background-color: #ecf5ff; border-color: #b3d8ff; color: #2d8cf0;
}
</style>
