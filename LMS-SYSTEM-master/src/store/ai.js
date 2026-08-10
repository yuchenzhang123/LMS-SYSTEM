import { aiChatApi, dailyBriefingApi } from '@/api/ai'

const state = {
  chatMessages: [],       // { role: 'user'|'assistant', content, timestamp }
  chatLoading: false,
  dailyBriefing: '',      // 每日简报文字
  briefingLoading: false,
  briefingError: false
}

const mutations = {
  ADD_CHAT_MESSAGE(state, message) {
    state.chatMessages.push(message)
  },
  CLEAR_CHAT(state) {
    state.chatMessages = []
  },
  SET_CHAT_LOADING(state, loading) {
    state.chatLoading = loading
  },
  SET_BRIEFING(state, briefing) {
    state.dailyBriefing = briefing
    state.briefingError = false
  },
  SET_BRIEFING_LOADING(state, loading) {
    state.briefingLoading = loading
  },
  SET_BRIEFING_ERROR(state, error) {
    state.briefingError = error
  }
}

const actions = {
  async sendMessage({ commit, rootState }, question) {
    commit('ADD_CHAT_MESSAGE', { role: 'user', content: question, timestamp: Date.now() })
    commit('SET_CHAT_LOADING', true)

    const permission = rootState.permission
    try {
      const res = await aiChatApi({
        question,
        orgCode: permission.orgCode,
        ehrNo: permission.ehrNo
      })
      const data = res.data || res
      commit('ADD_CHAT_MESSAGE', {
        role: 'assistant',
        content: data.answer || '无法获取回答',
        data: data.data,
        timestamp: Date.now()
      })
    } catch (e) {
      commit('ADD_CHAT_MESSAGE', {
        role: 'assistant',
        content: '抱歉，AI 助手暂时无法响应，请稍后重试。',
        timestamp: Date.now()
      })
    } finally {
      commit('SET_CHAT_LOADING', false)
    }
  },

  async fetchDailyBriefing({ commit, rootState }) {
    commit('SET_BRIEFING_LOADING', true)
    const permission = rootState.permission
    try {
      const res = await dailyBriefingApi({
        orgCode: permission.orgCode,
        ehrNo: permission.ehrNo
      })
      const data = res.data || res
      commit('SET_BRIEFING', data.briefing || '暂无简报')
    } catch (e) {
      commit('SET_BRIEFING_ERROR', true)
    } finally {
      commit('SET_BRIEFING_LOADING', false)
    }
  },

  clearChat({ commit }) {
    commit('CLEAR_CHAT')
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
