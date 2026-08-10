<template>
  <div class="insight-container" v-loading="pageLoading">
    <!-- 每日简报 -->
    <el-card shadow="never" class="briefing-card" v-loading="briefingLoading">
      <div slot="header" class="card-header">
        <span>📰 每日简报</span>
        <el-button type="text" size="small" @click="refreshBriefing" :loading="briefingLoading">刷新</el-button>
      </div>
      <div class="briefing-content" v-if="dailyBriefing && !briefingError">
        {{ dailyBriefing }}
      </div>
      <div class="briefing-content text-muted" v-else-if="briefingError">
        简报加载失败，请稍后刷新
      </div>
    </el-card>

    <!-- Tab切换 -->
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="机构总览" name="org">
        <!-- 概览卡 -->
        <el-row :gutter="16" class="stats-row">
          <el-col :span="6" v-for="card in orgStatCards" :key="card.label">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-value" :style="{color: card.color}">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 机构排名 -->
        <el-card shadow="never" class="section-card" v-loading="rankingLoading">
          <div slot="header" class="card-header">
            <span>🏢 机构排名</span>
            <el-select v-model="timeRange" size="small" @change="fetchOrgRanking" style="width:120px">
              <el-option label="本月" value="month" />
              <el-option label="近7天" value="week" />
              <el-option label="近30天" value="month30" />
            </el-select>
          </div>
          <el-table :data="orgRanking" stripe size="small" empty-text="暂无数据">
            <el-table-column prop="branchName" label="机构" min-width="120" />
            <el-table-column prop="count" label="逾期数量" width="100" sortable />
            <el-table-column prop="totalAmt" label="逾期金额(万)" width="120" sortable>
              <template slot-scope="{row}">
                {{ formatAmt(row.totalAmt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="人员总览" name="user">
        <el-row :gutter="16" class="stats-row">
          <el-col :span="6" v-for="card in userStatCards" :key="card.label">
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
          </div>
          <el-table :data="userList" stripe size="small" empty-text="暂无数据">
            <el-table-column prop="userName" label="姓名" width="100" />
            <el-table-column prop="orgName" label="所属机构" min-width="120" />
            <el-table-column prop="orgCode" label="机构号" width="100" />
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
      </el-tab-pane>
    </el-tabs>

    <!-- AI 问答 -->
    <el-card shadow="never" class="section-card chat-card">
      <div slot="header" class="card-header">
        <span>💬 AI 分析</span>
      </div>
      <div class="chat-hints" v-if="chatMessages.length === 0">
        <span class="text-muted">💡 试试问：</span>
        <el-tag v-for="hint in chatHints" :key="hint" size="small" class="chat-hint-tag"
                @click="askQuestion(hint)">{{ hint }}</el-tag>
      </div>
      <div class="chat-messages" v-if="chatMessages.length > 0">
        <div v-for="(msg, idx) in chatMessages" :key="idx" :class="['chat-message', msg.role]">
          <div class="chat-role">{{ msg.role === 'user' ? '🤔 你' : '🤖 助手' }}</div>
          <div class="chat-content">{{ msg.content }}</div>
        </div>
      </div>
      <div class="chat-input">
        <el-input v-model="chatInput" placeholder="输入你的问题..." @keyup.enter.native="askQuestion(chatInput)"
                  :disabled="chatLoading" size="small">
          <el-button slot="append" icon="el-icon-s-promotion" @click="askQuestion(chatInput)"
                     :loading="chatLoading" :disabled="!chatInput.trim()" />
        </el-input>
      </div>
    </el-card>
  </div>
</template>

<script>
import { getAccountStatsApi } from '@/api/collection'
import { getUserListApi, getUserStatsApi } from '@/api/user'
import { getGroupTreeApi } from '@/api/org'

export default {
  name: 'Insight',
  data() {
    return {
      activeTab: 'org',
      pageLoading: false,
      briefingLoading: false,
      rankingLoading: false,
      userLoading: false,
      timeRange: 'month',

      dailyBriefing: '',
      briefingError: false,

      orgRanking: [],
      orgStatCards: [
        { label: '逾期账户数', value: '-', color: '#E6A23C' },
        { label: '逾期总余额', value: '-', color: '#F56C6C' },
        { label: '催收完成率', value: '-', color: '#67C23A' },
        { label: '同比变化', value: '-', color: '#409EFF' }
      ],
      userStatCards: [
        { label: '总人数', value: '-', color: '#409EFF' },
        { label: '活跃率(30天)', value: '-', color: '#67C23A' },
        { label: '活跃率(7天)', value: '-', color: '#E6A23C' },
        { label: '人均催收量', value: '-', color: '#909399' }
      ],
      userList: [],
      userTotal: 0,
      userPage: 1,
      userPageSize: 10,

      chatMessages: [],
      chatLoading: false,
      chatInput: '',
      chatHints: [
        '为什么逾期率上升？',
        '哪个机构新增逾期最多？',
        '哪个员工效率最高？',
        '和海秀支行的逾期率为什么高？'
      ]
    }
  },
  computed: {
    orgCode() { return this.$store.state.permission.orgCode || '' },
    ehrNo() { return this.$store.state.permission.ehrNo || '' },
    userRole() { return this.$store.state.permission.userRole || '' }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.pageLoading = true
      this.refreshBriefing()
      await Promise.all([
        this.fetchOrgRanking(),
        this.fetchUserList()
      ])
      this.pageLoading = false
    },

    async refreshBriefing() {
      this.briefingLoading = true
      try {
        await this.$store.dispatch('ai/fetchDailyBriefing')
        this.dailyBriefing = this.$store.state.ai.dailyBriefing
        this.briefingError = this.$store.state.ai.briefingError
      } catch (e) {
        this.briefingError = true
      }
      this.briefingLoading = false
    },

    async fetchOrgRanking() {
      this.rankingLoading = true
      try {
        const res = await getAccountStatsApi({
          orgCode: this.userRole !== 'staff' ? this.orgCode : '',
          branchCode: this.userRole === 'staff' ? this.orgCode : ''
        })
        const data = res.data || res
        this.orgStatCards[0].value = data.activeCount || 0
        this.orgStatCards[1].value = data.totalLoanBalance || '0.00'
      } catch (e) {
        console.error('获取机构排名失败:', e)
      }
      this.rankingLoading = false
    },

    async fetchUserList() {
      this.userLoading = true
      try {
        const res = await getUserListApi({
          orgCode: this.userRole !== 'staff' ? this.orgCode : '',
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

    handleTabClick() {
      if (this.activeTab === 'user' && this.userList.length === 0) {
        this.fetchUserList()
      }
    },

    async askQuestion(question) {
      const q = typeof question === 'string' ? question.trim() : this.chatInput.trim()
      if (!q || this.chatLoading) return
      this.chatInput = ''
      await this.$store.dispatch('ai/sendMessage', q)
      this.chatMessages = this.$store.state.ai.chatMessages
      // 滚动到底部
      this.$nextTick(() => {
        const container = this.$el.querySelector('.chat-messages')
        if (container) container.scrollTop = container.scrollHeight
      })
    },

    formatAmt(val) {
      if (!val) return '0'
      const n = Number(val)
      if (isNaN(n)) return String(val)
      return (n / 10000).toFixed(0)
    }
  }
}
</script>

<style scoped>
.insight-container { padding: 0; }
.briefing-card { margin-bottom: 16px; }
.briefing-content { line-height: 1.8; color: #303133; }
.text-muted { color: #909399; }

.card-header { display: flex; justify-content: space-between; align-items: center; }

.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }

.section-card { margin-bottom: 16px; }

.chat-card { margin-top: 8px; }
.chat-hints { margin-bottom: 12px; }
.chat-hint-tag { cursor: pointer; margin-right: 8px; margin-bottom: 8px; }
.chat-messages { max-height: 300px; overflow-y: auto; margin-bottom: 12px; }
.chat-message { margin-bottom: 12px; padding: 8px 12px; border-radius: 6px; }
.chat-message.user { background: #ecf5ff; }
.chat-message.assistant { background: #f5f7fa; }
.chat-role { font-weight: bold; font-size: 13px; margin-bottom: 4px; }
.chat-content { font-size: 14px; line-height: 1.6; }
.chat-input { margin-top: 8px; }
</style>
