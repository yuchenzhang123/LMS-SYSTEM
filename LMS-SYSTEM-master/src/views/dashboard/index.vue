<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in stats" :key="item.title">
        <el-card shadow="hover" :body-style="{ padding: '20px' }">
          <div class="stat-item">
            <i :class="item.icon" :style="{ color: item.color }"></i>
            <div class="stat-info">
              <div class="stat-title">{{ item.title }}</div>
              <div class="stat-value">{{ item.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="welcome-card" style="margin-top: 20px;">
      <div slot="header"><span>系统公告</span></div>
      <p>欢迎登录 **信贷催收管理系统**。当前系统版本：V1.0.26</p>
      <p style="color: #909399; font-size: 14px;">请及时跟进“催收中”状态的账户，确保回款率。</p>
    </el-card>
  </div>
</template>

<script>
export default {
  data () {
    return {
      stats: [
        { title: '待催收总数', value: '1,284', icon: 'el-icon-warning', color: '#E6A23C' },
        { title: '本月已回收', value: '¥ 458,000', icon: 'el-icon-success', color: '#67C23A' },
        { title: '我的待办', value: '12', icon: 'el-icon-message-solid', color: '#409EFF' },
        { title: '逾期率(M1)', value: '2.4%', icon: 'el-icon-pie-chart', color: '#F56C6C' }
      ]
    }
  }
}
</script>

<style scoped>
.stat-item { display: flex; align-items: center; }
.stat-item i { font-size: 40px; margin-right: 15px; }
.stat-title { font-size: 14px; color: #909399; }
.stat-value { font-size: 24px; font-weight: bold; margin-top: 5px; }
</style>
