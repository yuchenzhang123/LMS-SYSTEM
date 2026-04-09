<template>
  <div>
    <div class="toolbar-row">
      <div class="right-tools">
        <el-button v-if="!readOnly" size="small" type="primary" @click="$emit('openNewLitigation')">新增诉讼</el-button>
      </div>
    </div>

    <el-table :data="litigationList" border stripe style="width: 100%">
      <el-table-column prop="statusText" label="诉讼状态" min-width="200">
        <template slot-scope="scope">
          <el-tag :type="scope.row.inLitigation ? 'warning' : 'success'" size="small">
            {{ scope.row.statusText || '未登记' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="诉讼备注" min-width="200"></el-table-column>
      <el-table-column prop="updatedAt" label="最近更新时间" width="170"></el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="$emit('openLitigationDetail', scope.row)">
            查看详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
export default {
  name: 'LitigationTab',
  props: {
    litigationList: {
      type: Array,
      default: () => []
    },
    readOnly: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style scoped>
.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.right-tools {
  display: flex;
  gap: 8px;
}
</style>
