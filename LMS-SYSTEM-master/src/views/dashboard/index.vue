<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" :body-style="{ padding: '24px' }" v-loading="loading">
          <div class="stat-item">
            <i class="el-icon-user" style="color: #E6A23C;"></i>
            <div class="stat-info">
              <div class="stat-title">未完成催收客户数</div>
              <div class="stat-value">{{ stats.activeCount }}</div>
              <div class="stat-sub">未催收 + 催收中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" :body-style="{ padding: '24px' }" v-loading="loading">
          <div class="stat-item">
            <i class="el-icon-money" style="color: #F56C6C;"></i>
            <div class="stat-info">
              <div class="stat-title">贷款余额合计</div>
              <div class="stat-value">¥ {{ stats.totalLoanBalance }}</div>
              <div class="stat-sub">未完成催收账户</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="16">
        <el-card shadow="never">
          <div slot="header"><span>最近未读通知</span></div>
          <el-empty v-if="recentNotices.length === 0" description="暂无未读通知" :image-size="60"></el-empty>
          <el-table v-else :data="recentNotices" size="small" :show-header="false">
            <el-table-column width="80">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.noticeType === 'new_overdue' ? 'danger' : 'success'">
                  {{ scope.row.noticeType === 'new_overdue' ? '新增逾期' : '催收完成' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" min-width="200"></el-table-column>
            <el-table-column prop="createdAt" width="160" align="right">
              <template slot-scope="scope">
                <span style="color: #909399; font-size: 12px;">{{ scope.row.createdAt }}</span>
              </template>
            </el-table-column>
            <el-table-column width="80" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="goNotice(scope.row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="unreadNoticeCount > 5" style="text-align: right; margin-top: 8px;">
            <el-button type="text" size="small" @click="$router.push('/notice/list')">查看全部 {{ unreadNoticeCount }} 条未读 →</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never">
          <div slot="header"><span>账户状态分布</span></div>
          <div class="status-bar-list" v-loading="loading">
            <div class="status-bar-item" v-for="s in statusBars" :key="s.key">
              <div class="bar-label">
                <span>{{ s.label }}</span>
                <span class="bar-count">{{ s.count }}</span>
              </div>
              <el-progress
                :percentage="totalAccounts > 0 ? Math.round(s.count / totalAccounts * 100) : 0"
                :color="s.color"
                :stroke-width="10"
                :show-text="false"
              ></el-progress>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import { getAccountStatsApi, getAccountListApi, getNoticeListApi } from '@/api/collection'

export default {
  name: 'Dashboard',
  data () {
    return {
      loading: false,
      stats: { activeCount: 0, totalLoanBalance: '0.00' },
      counts: { uncollected: 0, collecting: 0, completed: 0 },
      recentNotices: []
    }
  },
  computed: {
    ...mapState('permission', ['userRole', 'orgCode']),
    ...mapGetters('collection', ['unreadNoticeCount']),
    totalAccounts () {
      return this.counts.uncollected + this.counts.collecting + this.counts.completed
    },
    statusBars () {
      return [
        { key: 'uncollected', label: '未催收', count: this.counts.uncollected, color: '#909399' },
        { key: 'collecting', label: '催收中', count: this.counts.collecting, color: '#E6A23C' },
        { key: 'completed', label: '已还款', count: this.counts.completed, color: '#67C23A' }
      ]
    }
  },
  created () {
    this.loadData()
  },
  methods: {
    async loadData () {
      this.loading = true
      try {
        const branchCode = this.userRole === 'staff' ? this.orgCode : ''
        const orgCode = this.userRole === 'manager' ? this.orgCode : ''
        const baseQuery = { branchCode, orgCode, page: { currentPage: 1, pageSize: 1 } }

        const [statsRes, uncollectedRes, collectingRes, completedRes, noticeRes] = await Promise.all([
          getAccountStatsApi({ branchCode, orgCode }),
          getAccountListApi({ ...baseQuery, status: 'uncollected' }),
          getAccountListApi({ ...baseQuery, status: 'collecting' }),
          getAccountListApi({ ...baseQuery, status: 'completed' }),
          getNoticeListApi({ readStatus: 0, branchCode, page: { currentPage: 1, pageSize: 5 } })
        ])

        const statsData = statsRes.data || statsRes
        this.stats.activeCount = statsData.activeCount || 0
        this.stats.totalLoanBalance = statsData.totalLoanBalance || '0.00'

        this.counts.uncollected = Number((uncollectedRes.data || uncollectedRes).total || 0)
        this.counts.collecting = Number((collectingRes.data || collectingRes).total || 0)
        this.counts.completed = Number((completedRes.data || completedRes).total || 0)

        const noticeData = noticeRes.data || noticeRes
        this.recentNotices = noticeData.records || []
        this.$store.commit('collection/SET_NOTICES', {
          records: this.recentNotices,
          total: noticeData.total || 0,
          unreadCount: noticeData.unreadCount || 0
        })
      } catch (e) {
        // eslint-disable-next-line no-console
        console.warn('Dashboard 数据加载失败:', e.message)
      } finally {
        this.loading = false
      }
    },
    goNotice (notice) {
      this.$store.dispatch('collection/openNotice', notice)
      this.$router.push('/notice/detail')
    }
  }
}
</script>

<style scoped>
.dashboard-container { padding: 10px; }
.stat-item { display: flex; align-items: center; }
.stat-item i { font-size: 40px; margin-right: 20px; }
.stat-title { font-size: 13px; color: #909399; }
.stat-value { font-size: 28px; font-weight: bold; margin-top: 4px; color: #303133; }
.stat-sub { font-size: 12px; color: #c0c4cc; margin-top: 4px; }

.status-bar-list { padding: 4px 0; }
.status-bar-item { margin-bottom: 18px; }
.bar-label { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 13px; color: #606266; }
.bar-count { font-weight: bold; color: #303133; }
</style>
