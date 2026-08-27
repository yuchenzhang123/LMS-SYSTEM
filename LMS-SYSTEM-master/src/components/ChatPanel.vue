<template>
  <div class="chat-panel">
    <!-- 头部（可插槽覆盖） -->
    <div class="chat-panel-header">
      <slot name="header">💬 AI 分析</slot>
    </div>

    <!-- 提示语（无消息且非思考中时展示，可插槽自定义） -->
    <div class="chat-hints" v-if="messages.length === 0 && !loading">
      <slot name="hints">
        <span class="hint-tip">💡 输入问题，开始 AI 分析</span>
      </slot>
    </div>

    <!-- 消息区：用户靠右、助手靠左 -->
    <div class="chat-messages" v-if="messages.length > 0 || loading">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['chat-message', msg.role === 'user' ? 'is-user' : 'is-assistant']"
      >
        <div class="chat-bubble">
          <div class="chat-content">{{ msg.content }}</div>
        </div>
      </div>

      <!-- 等待响应：思考中 -->
      <div class="chat-message is-assistant" v-if="loading">
        <div class="chat-bubble">
          <div class="chat-content thinking">
            思考中<span class="thinking-dots"><i>.</i><i>.</i><i>.</i></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区（作用域插槽暴露 loading/send，可自定义） -->
    <div class="chat-input">
      <slot name="input" :loading="loading" :send="emitSend">
        <el-input
          v-model="inputText"
          placeholder="输入你的问题..."
          @keyup.enter.native="emitSend"
          :disabled="loading"
          size="small"
        >
          <el-button
            slot="append"
            icon="el-icon-s-promotion"
            @click="emitSend"
            :loading="loading"
            :disabled="!inputText.trim()"
          />
        </el-input>
      </slot>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChatPanel',
  props: {
    // 消息列表: [{ role: 'user'|'assistant', content: String }]
    messages: { type: Array, default: () => [] },
    // 是否等待后端响应（展示「思考中」）
    loading: { type: Boolean, default: false }
  },
  data() {
    return { inputText: '' }
  },
  methods: {
    emitSend() {
      const q = this.inputText.trim()
      if (!q || this.loading) return
      this.inputText = ''
      this.$emit('send', q)
    }
  }
}
</script>

<style scoped>
.chat-panel { display: flex; flex-direction: column; }

.chat-panel-header {
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}

.chat-hints { margin-bottom: 12px; }
.hint-tip { color: #909399; }

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 320px;
  overflow-y: auto;
  margin-bottom: 12px;
}

.chat-message { display: flex; }
.chat-message.is-user { justify-content: flex-end; }
.chat-message.is-assistant { justify-content: flex-start; }

.chat-bubble {
  max-width: 76%;
  padding: 8px 12px;
  border-radius: 10px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.is-user .chat-bubble {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 2px;
}
.is-assistant .chat-bubble {
  background: #f5f7fa;
  color: #303133;
  border-top-left-radius: 2px;
}

.chat-content { white-space: pre-wrap; }
.chat-content.thinking { color: #909399; }

.thinking-dots i {
  font-style: normal;
  animation: thinking-blink 1.4s infinite both;
}
.thinking-dots i:nth-child(2) { animation-delay: 0.2s; }
.thinking-dots i:nth-child(3) { animation-delay: 0.4s; }

@keyframes thinking-blink {
  0%, 80%, 100% { opacity: 0.2; }
  40% { opacity: 1; }
}

.chat-input { margin-top: 8px; }
</style>
