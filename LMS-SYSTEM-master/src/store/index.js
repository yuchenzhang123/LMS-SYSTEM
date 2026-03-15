import Vue from 'vue'
import Vuex from 'vuex'
import permission from './permission'
import collection from './collection'

// 1. 必须先安装插件
Vue.use(Vuex)

// 2. 创建 Store 实例并导出
const store = new Vuex.Store({
  modules: {
    // 关键点：这里的 key 必须叫 permission
    // 这样在路由守卫里才能通过 store.state.permission 访问到数据
    permission,
    collection
  },
  // 开启严格模式（仅在开发环境），帮助你发现不规范的状态修改
  strict: process.env.NODE_ENV !== 'production'
})

export default store
