<template>
  <div class="unauthorized-container">
    <div class="unauthorized-card">
      <i class="el-icon-lock icon"></i>
      <h2 class="title">暂无访问权限</h2>
      <p class="desc">您的账号尚未被分配系统权限，如有需求请联系管理员添加。</p>
      <div class="info-box">
        <span class="info-label">当前机构号：</span>
        <span class="info-value">{{ orgCode || '未获取' }}</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Unauthorized',
  computed: {
    orgCode () {
      return this.$store && this.$store.state.permission && this.$store.state.permission.orgCode
    }
  }
}
</script>

<style scoped>
.unauthorized-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}
.unauthorized-card {
  text-align: center;
  padding: 60px 80px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.icon {
  font-size: 64px;
  color: #E6A23C;
  display: block;
  margin-bottom: 20px;
}
.title {
  font-size: 22px;
  color: #303133;
  margin: 0 0 12px;
}
.desc {
  font-size: 14px;
  color: #606266;
  margin: 0 0 24px;
}
.info-box {
  display: inline-block;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 8px 20px;
  font-size: 13px;
}
.info-label { color: #909399; }
.info-value { color: #303133; font-family: monospace; }
</style>
