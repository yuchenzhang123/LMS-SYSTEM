<template>
  <el-card shadow="never" class="briefing-card" v-loading="briefingLoading">
    <div slot="header" class="card-header">
      <span>📰 每日简报</span>
      <el-button type="text" size="small" @click="refreshBriefing(true)" :loading="briefingLoading">刷新</el-button>
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
    async refreshBriefing (force = false) {
      this.briefingLoading = true
      try {
        // 刷新按钮显式传 force=true 强制重新生成（绕过缓存）；首次 created 加载默认 false 命中缓存
        await this.$store.dispatch('ai/fetchDailyBriefing', force)
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
