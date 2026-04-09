<template>
  <div>
    <div class="toolbar-row">
      <div class="right-tools">
        <template v-if="!readOnly">
          <el-button size="small" type="primary" @click="$emit('sendSms')" :loading="smsLoading">短信催收</el-button>
          <el-button size="small" @click="$emit('openRecordDialog')">登记催收记录</el-button>
        </template>
      </div>
    </div>

    <el-table :data="records" border stripe style="width: 100%">
      <el-table-column prop="time" label="催收时间" width="170"></el-table-column>
      <el-table-column prop="methodText" label="方式" width="90"></el-table-column>
      <el-table-column prop="result" label="催收结果" min-width="220"></el-table-column>
      <el-table-column prop="operatorName" label="业务员" width="110"></el-table-column>
      <el-table-column prop="remark" label="备注" min-width="180"></el-table-column>
      <el-table-column prop="materialName" label="附件材料" min-width="180"></el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button
            type="text"
            size="mini"
            :disabled="!scope.row.materialUrl"
            @click="$emit('exportMaterial', scope.row)"
            style="margin-right: 8px;"
          >
            导出
          </el-button>
          <el-button
            type="text"
            size="mini"
            @click="$emit('openMaterialUpdateDialog', scope.row)"
          >
            {{ scope.row.materialUrl ? '重交材料' : '补交材料' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
export default {
  name: 'CollectionRecordTab',
  props: {
    records: {
      type: Array,
      default: () => []
    },
    smsLoading: {
      type: Boolean,
      default: false
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
