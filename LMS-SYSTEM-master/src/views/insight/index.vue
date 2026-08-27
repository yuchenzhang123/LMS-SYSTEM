<template>
  <div class="insight-container">
    <!-- 每日简报 -->
    <briefing-card />

    <!-- Tab切换 -->
    <el-tabs v-model="activeTab">
      <el-tab-pane label="机构总览" name="org">
        <org-overview-tab />
      </el-tab-pane>

      <el-tab-pane label="人员总览" name="user">
        <user-overview-tab />
      </el-tab-pane>
    </el-tabs>

    <!-- AI 问答 -->
    <el-card shadow="never" class="section-card chat-card">
      <ChatPanel :messages="chatMessages" :loading="chatLoading" @send="askQuestion">
        <template slot="hints">
          <span class="text-muted">💡 试试问：</span>
          <el-tag v-for="hint in chatHints" :key="hint" size="small" class="chat-hint-tag"
                  @click="askQuestion(hint)">{{ hint }}</el-tag>
        </template>
      </ChatPanel>
    </el-card>
  </div>
</template>

<script>
import { mapState } from 'vuex'
import ChatPanel from '@/components/ChatPanel'
import BriefingCard from '@/components/insight/BriefingCard.vue'
import OrgOverviewTab from '@/components/insight/OrgOverviewTab.vue'
import UserOverviewTab from '@/components/insight/UserOverviewTab.vue'

export default {
  name: 'Insight',
  components: { ChatPanel, BriefingCard, OrgOverviewTab, UserOverviewTab },
  data () {
    return {
      activeTab: 'org',
      chatHints: [
        '为什么逾期率上升？',
        '哪个机构新增逾期最多？',
        '哪个员工效率最高？',
        '和海秀支行的逾期率为什么高？'
      ]
    }
  },
  computed: {
    ...mapState('ai', ['chatMessages', 'chatLoading'])
  },
  methods: {
    askQuestion (question) {
      const q = typeof question === 'string' ? question.trim() : ''
      if (!q || this.chatLoading) return
      this.$store.dispatch('ai/sendMessage', q)
      this.$nextTick(() => {
        const container = this.$el.querySelector('.chat-messages')
        if (container) container.scrollTop = container.scrollHeight
      })
    }
  }
}
</script>

<style scoped>
.insight-container { padding: 0; }
.text-muted { color: #909399; }
.section-card { margin-bottom: 16px; }
.chat-card { margin-top: 8px; }
.chat-hint-tag { cursor: pointer; margin-right: 8px; margin-bottom: 8px; }
</style>
