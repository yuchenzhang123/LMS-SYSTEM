<template>
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
</template>

<script>
export default {
  name: 'BriefingCard',
  data () {
    return {
      briefingLoading: false,
      dailyBriefing: '',
      briefingError: false
    }
  },
  created () {
    this.refreshBriefing()
  },
  methods: {
    async refreshBriefing () {
      this.briefingLoading = true
      try {
        await this.$store.dispatch('ai/fetchDailyBriefing')
        this.dailyBriefing = this.$store.state.ai.dailyBriefing
        this.briefingError = this.$store.state.ai.briefingError
      } catch (e) {
        this.briefingError = true
      }
      this.briefingLoading = false
    }
  }
}
</script>

<style scoped>
.briefing-card { margin-bottom: 16px; }
.briefing-content { line-height: 1.8; color: #303133; }
.text-muted { color: #909399; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
