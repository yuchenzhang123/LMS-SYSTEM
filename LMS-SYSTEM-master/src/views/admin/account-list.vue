<template>
  <div class="account-container">
    <el-card shadow="never" class="filter-card">
      <el-tabs v-model="activeStatus" @tab-click="handleTabChange">
        <el-tab-pane label="未催收" name="uncollected"></el-tab-pane>
        <el-tab-pane label="催收中" name="collecting"></el-tab-pane>
        <el-tab-pane label="已还款" name="completed"></el-tab-pane>
      </el-tabs>

      <el-form :inline="true" :model="queryForm" class="search-form" size="small">
        <el-form-item label="业务机构">
          <el-select
            v-model="selectedBranchCode" placeholder="请选择业务机构"
            clearable filterable style="width: 220px" @change="handleBranchChange"
          >
            <el-option label="全部机构" value=""></el-option>
            <template v-if="isAdmin">
              <el-option-group v-for="g in branchGroups" :key="g.orgCode" :label="g.orgName">
                <el-option v-for="b in g.branches" :key="b.branchCode" :label="b.branchName" :value="b.branchCode" />
              </el-option-group>
            </template>
            <template v-else>
              <el-option v-for="b in branchOptions" :key="b.branchCode" :label="b.branchName" :value="b.branchCode" />
            </template>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button size="small" icon="el-icon-download" @click="exportDialogVisible = true">导出</el-button>
          <el-button size="small" icon="el-icon-folder-opened" @click="downloadCenterVisible = true">下载中心</el-button>
        </el-form-item>
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
            <el-button class="action-enter-btn" size="mini" type="primary" plain @click="goDetail(scope.row)">查看详情</el-button>
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
import { getBranchesByOrgCodeApi, getOrgTreeApi } from '@/api/org'

export default {
  name: 'AdminAccountList',
  components: { ExportDialog, DownloadCenter },
  mixins: [accountListMixin],
  data () {
    return {
      activeStatus: 'uncollected',
      loading: false,
      selectedBranchCode: '',
      branchOptions: [],
      branchGroups: [],
      queryForm: { customerId: '', loanAccount: '', productCode: '', overdueDays: undefined },
      tableData: [],
      page: { currentPage: 1, pageSize: 10, total: 0 },
      syncTimer: null,
      exportDialogVisible: false,
      downloadCenterVisible: false
    }
  },
  computed: {
    isAdmin () {
      return this.$store.state.permission.userRole === 'admin'
    }
  },
  async created () {
    await this.loadBranchOptions()
    this.restoreStateFromStore()
    this.fetchData()
  },
  watch: {
    activeStatus: 'scheduleSync',
    selectedBranchCode: 'scheduleSync',
    'queryForm.customerId': 'scheduleSync',
    'queryForm.loanAccount': 'scheduleSync',
    'queryForm.productCode': 'scheduleSync',
    'queryForm.overdueDays': 'scheduleSync',
    'page.currentPage': 'scheduleSync',
    'page.pageSize': 'scheduleSync'
  },
  methods: {
    scheduleSync () {
      if (this.restoringStoreState) return
      clearTimeout(this.syncTimer)
      this.syncTimer = setTimeout(() => this.syncListStateToStore(), 150)
    },
    async loadBranchOptions () {
      const { orgCode, userRole } = this.$store.state.permission
      if (!orgCode || orgCode === 'DEV_ADMIN' || orgCode === 'DEV_ORG') return
      try {
        if (userRole === 'admin') {
          const res = await getOrgTreeApi()
          const tree = res.data || res || []
          this.branchGroups = tree
            .filter(j => j.children && j.children.length > 0)
            .map(j => ({
              orgCode: j.orgCode,
              orgName: j.orgName,
              branches: j.children.map(b => ({ branchCode: b.branchCode, branchName: b.branchName }))
            }))
        } else {
          const res = await getBranchesByOrgCodeApi(orgCode)
          this.branchOptions = res.data || res || []
        }
      } catch (e) {
        // eslint-disable-next-line no-console
        console.warn('获取业务机构列表失败', e)
      }
    },
    restoreStateFromStore () {
      this.restoringStoreState = true
      const s = this.$store.state.collection && this.$store.state.collection.adminListState
      if (s) {
        this.activeStatus = s.activeStatus || 'uncollected'
        this.selectedBranchCode = s.selectedBranchCode || ''
        this.queryForm = {
          customerId: s.queryForm.customerId || '',
          loanAccount: s.queryForm.loanAccount || '',
          productCode: s.queryForm.productCode || '',
          overdueDays: s.queryForm.overdueDays
        }
        this.page.currentPage = Number(s.page.currentPage) || 1
        this.page.pageSize = Number(s.page.pageSize) || 10
        this.listScrollY = Number(s.scrollY || 0)
        this.shouldRestoreScroll = this.listScrollY > 0
      }
      this.$nextTick(() => { this.restoringStoreState = false })
    },
    syncListStateToStore () {
      if (this.restoringStoreState) return
      this.$store.commit('collection/SET_ADMIN_LIST_STATE', {
        activeStatus: this.activeStatus,
        selectedBranchCode: this.selectedBranchCode,
        queryForm: { ...this.queryForm },
        page: { currentPage: this.page.currentPage, pageSize: this.page.pageSize },
        scrollY: window.pageYOffset || 0
      })
    },
    handleBranchChange () {
      this.page.currentPage = 1
      this.fetchData()
    },
    async fetchData () {
      this.loading = true
      try {
        const { orgCode, userRole } = this.$store.state.permission
        const branchCode = this.selectedBranchCode || ''
        const queryOrgCode = (!branchCode && userRole === 'manager') ? orgCode : ''
        const data = await this.$store.dispatch('collection/fetchAccountList', {
          queryForm: { ...this.queryForm, status: this.activeStatus, branchCode, orgCode: queryOrgCode },
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
        this.loading = false
        this.afterFetch()
      }
    },
    resetQuery () {
      this.selectedBranchCode = ''
      this.queryForm = { customerId: '', loanAccount: '', productCode: '', overdueDays: undefined }
      this.page.currentPage = 1
      this.fetchData()
    }
  }
}
</script>

<style scoped>
.account-container { padding: 10px; }
.filter-card { margin-bottom: 15px; }
.search-form { margin-top: 20px; border-top: 1px solid #f0f0f0; padding-top: 20px; }
.table-card { min-height: 500px; }
.action-enter-btn {
  min-width: 86px; padding: 6px 10px;
  border-color: #d9ecff; background-color: #f5f9ff; color: #409EFF;
}
.action-enter-btn:hover, .action-enter-btn:focus {
  background-color: #ecf5ff; border-color: #b3d8ff; color: #2d8cf0;
}
</style>
