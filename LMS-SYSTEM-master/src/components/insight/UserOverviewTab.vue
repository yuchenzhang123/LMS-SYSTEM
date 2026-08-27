<template>
  <div>
    <el-row :gutter="16" class="stats-row">
      <el-col :span="8" v-for="card in userStatCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value" :style="{color: card.color}">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 人员列表 -->
    <el-card shadow="never" class="section-card" v-loading="userLoading">
      <div slot="header" class="card-header">
        <span>👥 人员列表</span>
        <el-select v-model="userTimeRange" size="small" @change="handleUserTimeRangeChange" style="width:120px">
          <el-option label="全部" value="all" />
          <el-option label="近7天" value="week" />
          <el-option label="近30天" value="month30" />
        </el-select>
      </div>
      <el-table :data="userList" stripe size="small" empty-text="暂无数据">
        <el-table-column prop="userName" label="姓名" width="100" />
        <el-table-column prop="orgName" label="所属机构" min-width="120" />
        <el-table-column prop="orgCode" label="机构号" width="100" />
        <el-table-column prop="collectionCount" label="催收数量" width="90" />
        <el-table-column label="活跃(30天)" width="100">
          <template slot-scope="{row}">
            <el-tag :type="row.active30d ? 'success' : 'info'" size="mini">{{ row.active30d ? '活跃' : '未活跃' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="活跃(7天)" width="100">
          <template slot-scope="{row}">
            <el-tag :type="row.active7d ? 'success' : 'info'" size="mini">{{ row.active7d ? '活跃' : '未活跃' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLogin" label="最后登录" width="160" />
      </el-table>
      <el-pagination
        v-if="userTotal > 0"
        layout="prev, pager, next"
        :total="userTotal"
        :page-size="userPageSize"
        :current-page.sync="userPage"
        @current-change="fetchUserList"
        style="margin-top:12px; text-align:right" />
    </el-card>
  </div>
</template>

<script>
import { getUserListApi, getUserStatsApi } from '@/api/user'

export default {
  name: 'UserOverviewTab',
  data () {
    return {
      userLoading: false,
      userStatCards: [
        { label: '总人数', value: '-', color: '#409EFF' },
        { label: '活跃率(30天)', value: '-', color: '#67C23A' },
        { label: '活跃率(7天)', value: '-', color: '#E6A23C' }
      ],
      userList: [],
      userTotal: 0,
      userPage: 1,
      userPageSize: 10,
      userTimeRange: 'all'
    }
  },
  computed: {
    orgCode () { return this.$store.state.permission.orgCode || '' },
    ehrNo () { return this.$store.state.permission.ehrNo || '' }
  },
  created () {
    this.fetchUserStats()
    this.fetchUserList()
  },
  methods: {
    async fetchUserStats () {
      try {
        const res = await getUserStatsApi({ orgCode: this.orgCode, ehrNo: this.ehrNo })
        const data = res.data || res
        this.userStatCards[0].value = data.totalUsers || 0
        this.userStatCards[1].value = (data.activeRate30d || 0) + '%'
        this.userStatCards[2].value = (data.activeRate7d || 0) + '%'
      } catch (e) {
        console.error('获取人员统计失败:', e)
      }
    },

    async fetchUserList () {
      this.userLoading = true
      try {
        const range = this.buildDateRange()
        const res = await getUserListApi({
          orgCode: this.orgCode,
          ehrNo: this.ehrNo,
          startDate: range.startDate,
          endDate: range.endDate,
          page: this.userPage,
          size: this.userPageSize
        })
        const data = res.data || res
        this.userList = data.records || []
        this.userTotal = data.total || 0
      } catch (e) {
        console.error('获取人员列表失败:', e)
      }
      this.userLoading = false
    },

    handleUserTimeRangeChange () {
      this.userPage = 1
      this.fetchUserList()
    },

    buildDateRange () {
      const end = new Date()
      let start = null
      if (this.userTimeRange === 'week') {
        start = new Date()
        start.setDate(start.getDate() - 7)
      } else if (this.userTimeRange === 'month30') {
        start = new Date()
        start.setDate(start.getDate() - 30)
      }
      return {
        startDate: start ? this.formatDate(start) : undefined,
        endDate: this.formatDate(end)
      }
    },

    formatDate (d) {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    }
  }
}
</script>

<style scoped>
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }

.section-card { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
