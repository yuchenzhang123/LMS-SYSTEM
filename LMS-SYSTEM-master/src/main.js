import Vue from 'vue'
import App from './App.vue'
import store from './store'
import router from './router'

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

// 注册 Element UI
Vue.use(ElementUI, { size: 'small' })

Vue.config.productionTip = false

// 开发环境日志
if (process.env.NODE_ENV === 'development') {
  console.log('--- 催收系统运行环境信息 ---')
  console.log('Host:', window.location.host)
  console.log('SSO GUARD:', process.env.VUE_APP_ENABLE_SSO_GUARD)
}

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
