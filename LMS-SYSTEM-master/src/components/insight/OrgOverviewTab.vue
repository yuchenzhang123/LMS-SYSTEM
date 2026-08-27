<template>
  <el-row :gutter="16" class="stats-row">
    <el-col :span="12" v-for="card in orgStatCards" :key="card.label">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-value" :style="{color: card.color}">{{ card.value }}</div>
        <div class="stat-label">{{ card.label }}</div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script>
import { getAccountStatsApi } from '@/api/collection'

export default {
  name: 'OrgOverviewTab',
  data () {
    return {
      orgStatCards: [
        { label: '逾期账户数', value: '-', color: '#E6A23C' },
        { label: '逾期总余额', value: '-', color: '#F56C6C' }
      ]
    }
  },
  computed: {
    orgCode () { return this.$store.state.permission.orgCode || '' },
    ehrNo () { return this.$store.state.permission.ehrNo || '' }
  },
  created () {
    this.fetchOrgRanking()
  },
  methods: {
    async fetchOrgRanking () {
      try {
        const res = await getAccountStatsApi({
          orgCode: this.orgCode,
          ehrNo: this.ehrNo
        })
        const data = res.data || res
        this.orgStatCards[0].value = data.activeCount || 0
        this.orgStatCards[1].value = data.totalLoanBalance || '0.00'
      } catch (e) {
        console.error('获取机构统计失败:', e)
      }
    }
  }
}
</script>

<style scoped>
.stats-row { margin-bottom: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: bold; }
.stat-label { font-size: 13px; color: #909399; margin-top: 6px; }
</style>
