<template>
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

    <!-- 下载中心（子对话框） -->
    <el-dialog
      title="下载中心"
      :visible.sync="downloadCenterVisible"
      width="700px"
      append-to-body
      @opened="fetchTasks"
    >
      <el-table :data="tasks" v-loading="taskLoading" size="small">
        <el-table-column prop="fileName" label="文件名" min-width="200"></el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status === 'COMPLETED'" type="success">已完成</el-tag>
            <el-tag v-else-if="scope.row.status === 'RUNNING'" type="warning">导出中</el-tag>
            <el-tag v-else-if="scope.row.status === 'FAILED'" type="danger">失败</el-tag>
            <el-tag v-else type="info">等待中</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="100">
          <template slot-scope="scope">
            {{ scope.row.fileSize ? formatSize(scope.row.fileSize) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170"></el-table-column>
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button v-if="scope.row.status === 'COMPLETED'" type="text" size="small"
              @click="downloadTask(scope.row)">下载</el-button>
            <el-button v-if="scope.row.status !== 'RUNNING'" type="text" size="small"
              @click="deleteTask(scope.row.taskId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="tasks.length === 0 && !taskLoading" style="text-align:center;color:#999;padding:20px">
        暂无导出记录
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { exportAccountApi, exportAccountAsyncApi, listExportTasksApi, downloadExportApi, deleteExportTaskApi } from '@/api/collection'
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
      exportMode: 'direct',
      exporting: false,
      // 下载中心
      downloadCenterVisible: false,
      tasks: [],
      taskLoading: false,
      _pollTimer: null
    }
  },
  computed: {
    dialogVisible: {
      get () { return this.visible },
      set (val) { this.$emit('update:visible', val) }
    }
  },
  beforeDestroy () {
    if (this._pollTimer) clearInterval(this._pollTimer)
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
        const res = await exportAccountAsyncApi(this.buildPayload())
        const task = res.data || res
        Message.success('已提交后台导出，请到下载中心查看')
        this.downloadCenterVisible = true
        this._startPolling()
      } catch (e) {
        Message.error('提交失败：' + (e.message || '未知错误'))
      } finally {
        this.exporting = false
      }
    },
    async fetchTasks () {
      this.taskLoading = true
      try {
        const res = await listExportTasksApi()
        this.tasks = res.data || res || []
      } catch (e) {
        // ignore
      } finally {
        this.taskLoading = false
      }
    },
    async downloadTask (task) {
      try {
        const res = await downloadExportApi(task.taskId)
        const blob = res.data || res
        downloadBlob(blob, task.fileName || '导出.xlsx')
      } catch (e) {
        Message.error('下载失败')
      }
    },
    async deleteTask (taskId) {
      try {
        await deleteExportTaskApi(taskId)
        await this.fetchTasks()
      } catch (e) {
        Message.error('删除失败')
      }
    },
    _startPolling () {
      this.fetchTasks()
      if (this._pollTimer) clearInterval(this._pollTimer)
      this._pollTimer = setInterval(() => {
        this.fetchTasks()
        // 全部任务不再有 RUNNING/PENDING 时停止轮询
        const hasActive = this.tasks.some(t => t.status === 'RUNNING' || t.status === 'PENDING')
        if (!hasActive) {
          clearInterval(this._pollTimer)
          this._pollTimer = null
        }
      }, 5000)
    },
    resetForm () {
      this.form = { statuses: ['uncollected', 'collecting'], dateRange: null }
      this.exportMode = 'direct'
    },
    formatSize (bytes) {
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / 1048576).toFixed(1) + ' MB'
    }
  }
}
</script>

<style scoped>
.export-hint { font-size: 12px; color: #909399; margin: 0; }
</style>
