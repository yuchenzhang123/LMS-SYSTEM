<template>
  <el-dialog
    title="下载中心"
    :visible.sync="dialogVisible"
    width="700px"
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
</template>

<script>
import { listExportTasksApi, downloadExportApi, deleteExportTaskApi } from '@/api/collection'
import { downloadBlob } from '@/utils/file-download'
import { Message } from 'element-ui'

export default {
  name: 'DownloadCenter',
  props: {
    visible: { type: Boolean, default: false }
  },
  data () {
    return {
      tasks: [],
      taskLoading: false,
      pollTimer: null
    }
  },
  computed: {
    dialogVisible: {
      get () { return this.visible },
      set (val) { this.$emit('update:visible', val) }
    }
  },
  beforeDestroy () {
    if (this.pollTimer) clearInterval(this.pollTimer)
  },
  methods: {
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
    startPolling () {
      this.fetchTasks()
      if (this.pollTimer) clearInterval(this.pollTimer)
      this.pollTimer = setInterval(() => {
        this.fetchTasks()
        const hasActive = this.tasks.some(t => t.status === 'RUNNING' || t.status === 'PENDING')
        if (!hasActive) {
          clearInterval(this.pollTimer)
          this.pollTimer = null
        }
      }, 5000)
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
    formatSize (bytes) {
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / 1048576).toFixed(1) + ' MB'
    }
  }
}
</script>
