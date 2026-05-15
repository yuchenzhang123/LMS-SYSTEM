<template>
  <el-dialog
    title="导出账户数据"
    :visible.sync="dialogVisible"
    width="480px"
    :close-on-click-modal="false"
    :close-on-press-escape="!exporting"
    :show-close="!exporting"
    @close="resetForm"
  >
    <div v-loading="exporting" element-loading-text="正在导出，请稍候..." element-loading-spinner="el-icon-loading">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账户状态">
          <el-checkbox-group v-model="form.statuses">
            <el-checkbox label="uncollected">未催收</el-checkbox>
            <el-checkbox label="collecting">催收中</el-checkbox>
            <el-checkbox label="completed">已还款</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="放款时间">
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="截止日期"
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="导出说明">
          <p class="export-hint">
            导出不含联系电话；如账户有诉讼信息则带最近一条诉讼记录；带最近一条催收记录。
          </p>
        </el-form-item>
      </el-form>
    </div>

    <div slot="footer">
      <el-button @click="dialogVisible = false" :disabled="exporting">取消</el-button>
      <el-button type="primary" :loading="exporting" @click="handleExport">{{ exporting ? '导出中...' : '导出' }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { exportAccountApi } from '@/api/collection'
import { downloadBlob } from '@/utils/file-download'
import { Message } from 'element-ui'

export default {
  name: 'ExportDialog',
  props: {
    visible: { type: Boolean, default: false }
  },
  data () {
    return {
      form: { statuses: ['uncollected', 'collecting'], dateRange: null },
      exporting: false
    }
  },
  computed: {
    dialogVisible: {
      get () { return this.visible },
      set (val) { this.$emit('update:visible', val) }
    }
  },
  methods: {
    async handleExport () {
      this.exporting = true
      try {
        const payload = { statuses: this.form.statuses }
        if (this.form.dateRange && this.form.dateRange.length === 2) {
          payload.startDate = this.form.dateRange[0]
          payload.endDate = this.form.dateRange[1]
        }
        const res = await exportAccountApi(payload)
        const blob = res.data || res

        // 后端返回错误时可能仍是 JSON 而非 Excel
        if (blob.type && blob.type.includes('json')) {
          const text = await new Response(blob).text()
          const err = JSON.parse(text)
          throw new Error(err.msg || err.message || '导出失败')
        }

        downloadBlob(blob, '催收账户导出.xlsx')
        Message.success('导出成功')
        this.dialogVisible = false
      } catch (e) {
        Message.error('导出失败：' + (e.message || '未知错误'))
      } finally {
        this.exporting = false
      }
    },
    resetForm () {
      this.form = { statuses: ['uncollected', 'collecting'], dateRange: null }
    }
  }
}
</script>

<style scoped>
.export-hint { font-size: 12px; color: #909399; margin: 0; }
</style>
