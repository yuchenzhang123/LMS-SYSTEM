<template>
  <div>
    <el-dialog
      title="导出账户数据"
      :visible.sync="dialogVisible"
      width="520px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
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
        <el-form-item label="导出方式">
          <el-radio-group v-model="exportMode">
            <el-radio label="direct">直接下载（等待完成）</el-radio>
            <el-radio label="async">后台导出（完成后在下载中心取）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="导出说明">
          <p class="export-hint">
            导出不含联系电话；如账户有诉讼信息则带最近一条诉讼记录；带最近一条催收记录。
          </p>
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false" :disabled="exporting">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="handleExport">
          {{ exporting ? '导出中...' : (exportMode === 'async' ? '提交后台导出' : '导出') }}
        </el-button>
      </div>
    </el-dialog>

    <!-- 下载中心引用 ExportDialog 内的 polling 通知 -->
    <DownloadCenter :visible.sync="downloadCenterVisible" ref="downloadCenter" />
  </div>
</template>

<script>
import { exportAccountApi, exportAccountAsyncApi } from '@/api/collection'
import { downloadBlob } from '@/utils/file-download'
import { Message } from 'element-ui'
import DownloadCenter from './DownloadCenter.vue'

export default {
  name: 'ExportDialog',
  components: { DownloadCenter },
  props: {
    visible: { type: Boolean, default: false }
  },
  data () {
    return {
      form: { statuses: ['uncollected', 'collecting'], dateRange: null },
      exportMode: 'direct',
      exporting: false,
      downloadCenterVisible: false
    }
  },
  computed: {
    dialogVisible: {
      get () { return this.visible },
      set (val) { this.$emit('update:visible', val) }
    }
  },
  methods: {
    buildPayload () {
      const payload = { statuses: this.form.statuses }
      if (this.form.dateRange && this.form.dateRange.length === 2) {
        payload.startDate = this.form.dateRange[0]
        payload.endDate = this.form.dateRange[1]
      }
      return payload
    },
    async handleExport () {
      if (this.exportMode === 'async') {
        await this.asyncExport()
      } else {
        await this.directExport()
      }
    },
    async directExport () {
      this.exporting = true
      try {
        const res = await exportAccountApi(this.buildPayload())
        const blob = res.data || res
        downloadBlob(blob, '催收账户导出.xlsx')
        Message.success('导出成功')
        this.dialogVisible = false
      } catch (e) {
        Message.error('导出失败：' + (e.message || '未知错误'))
      } finally {
        this.exporting = false
      }
    },
    async asyncExport () {
      this.exporting = true
      try {
        await exportAccountAsyncApi(this.buildPayload())
        Message.success('已提交后台导出，请到下载中心查看')
        this.downloadCenterVisible = true
        if (this.$refs.downloadCenter) {
          this.$refs.downloadCenter.startPolling()
        }
      } catch (e) {
        Message.error('提交失败：' + (e.message || '未知错误'))
      } finally {
        this.exporting = false
      }
    },
    resetForm () {
      this.form = { statuses: ['uncollected', 'collecting'], dateRange: null }
      this.exportMode = 'direct'
    }
  }
}
</script>

<style scoped>
.export-hint { font-size: 12px; color: #909399; margin: 0; }
</style>
