<template>
  <div class="preview-container">
    <el-result icon="info" title="页面开发中" subTitle="后端已配置该菜单，但前端对应的业务组件尚未创建">
      <template slot="extra">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="菜单名称">{{ $route.meta.title }}</el-descriptions-item>
          <el-descriptions-item label="路由路径">{{ $route.path }}</el-descriptions-item>
          <el-descriptions-item label="预期文件位置">
            <code>src/views{{ $route.path }}/index.vue</code> 或
            <code>src/views{{ $route.path }}.vue</code>
          </el-descriptions-item>
        </el-descriptions>
        <el-button type="primary" size="medium" @click="$router.push('/')" style="margin-top: 20px;">
          返回首页
        </el-button>
      </template>
    </el-result>
  </div>
</template>

<style scoped>
.preview-container {
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  height: 100%;
}
code { color: #f56c6c; background: #fef0f0; padding: 2px 4px; }
</style>
