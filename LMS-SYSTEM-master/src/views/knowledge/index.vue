<template>
  <div class="knowledge-container">
    <div class="toolbar-row">
      <span class="page-title">知识库管理</span>
      <div>
        <el-button size="small" icon="el-icon-upload2" @click="openImport">上传文件导入</el-button>
        <el-button size="small" icon="el-icon-plus" type="primary" @click="openAdd">新增知识</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="list" border size="small" style="width: 100%">
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="140" :formatter="categoryLabel" />
      <el-table-column prop="chunkCount" label="片段数" width="90" align="center" />
      <el-table-column prop="updatedAt" label="更新时间" width="180" />
      <el-table-column label="操作" width="150" align="center">
        <template slot-scope="{ row }">
          <el-button type="text" size="mini" @click="openEdit(row)">编辑</el-button>
          <el-button type="text" size="mini" class="danger-text" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="editing ? '编辑知识' : '新增知识'" :visible.sync="dialogVisible" width="560px">
      <el-form label-width="80px" size="small">
        <el-form-item label="标题">
          <el-input v-model="form.title" :disabled="!!editing" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" clearable placeholder="选择分类（可选）" style="width: 100%">
            <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入知识正文（长文本自动切块）" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="submit">确定</el-button>
      </div>
    </el-dialog>

    <!-- 上传导入对话框 -->
    <el-dialog title="上传文件导入" :visible.sync="importVisible" width="500px">
      <el-form label-width="80px" size="small">
        <el-form-item label="分类">
          <el-select v-model="importForm.category" clearable placeholder="选择分类（可选）" style="width: 100%">
            <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            :auto-upload="false"
            :limit="1"
            :file-list="fileList"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            accept=".pdf,.doc,.docx,.txt,.md"
            action=""
          >
            <el-button size="small" icon="el-icon-folder-opened">选择文件</el-button>
            <span slot="tip" class="upload-tip">支持 pdf / doc / docx / txt / md，最大 10MB</span>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="importVisible = false">取消</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="submitImport">导入</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { Message } from 'element-ui'
import {
  listKnowledgeApi,
  getKnowledgeApi,
  addKnowledgeApi,
  importKnowledgeApi,
  updateKnowledgeApi,
  deleteKnowledgeApi
} from '@/api/knowledge'

// 知识分类枚举：schema=库信息 / sql-example=SQL用法示例 / business=业务知识。
// 其中 schema 与 sql-example 两类会被 NL2SQL 规划阶段 RAG 召回，business 仅用于 chat 分支。
const CATEGORY_OPTIONS = [
  { value: 'schema', label: '库信息' },
  { value: 'sql-example', label: 'SQL 用法示例' },
  { value: 'business', label: '业务知识' }
]

export default {
  name: 'KnowledgeIndex',
  data() {
    return {
      loading: false,
      submitting: false,
      list: [],
      dialogVisible: false,
      importVisible: false,
      editing: null,
      categoryOptions: CATEGORY_OPTIONS,
      form: { title: '', category: '', content: '' },
      importForm: { category: '' },
      fileList: [],
      selectedFile: null
    }
  },
  created() { this.loadList() },
  methods: {
    categoryLabel(row, column, cellValue) {
      if (!cellValue) return '—'
      const opt = CATEGORY_OPTIONS.find(o => o.value === cellValue)
      return opt ? opt.label : cellValue
    },

    async loadList() {
      this.loading = true
      try {
        const res = await listKnowledgeApi()
        this.list = res.data || res || []
      } catch (e) {
        // 错误已在 request 拦截器提示
      } finally {
        this.loading = false
      }
    },

    openAdd() {
      this.editing = null
      this.form = { title: '', category: '', content: '' }
      this.dialogVisible = true
    },

    async openEdit(row) {
      this.editing = row
      this.form = { title: row.title, category: row.category || '', content: '' }
      this.dialogVisible = true
      try {
        const res = await getKnowledgeApi(row.title)
        const data = res.data || res
        this.form.content = data.content || ''
        this.form.category = data.category || row.category || ''
      } catch (e) {
        // 原文加载失败不阻断编辑
      }
    },

    async submit() {
      if (!this.form.title.trim()) return Message.warning('请输入标题')
      if (!this.form.content.trim()) return Message.warning('请输入正文')
      this.submitting = true
      try {
        if (this.editing) {
          await updateKnowledgeApi(this.editing.title, this.form.category, this.form.content)
          Message.success('更新成功')
        } else {
          await addKnowledgeApi(this.form.title, this.form.category, this.form.content)
          Message.success('新增成功')
        }
        this.dialogVisible = false
        this.loadList()
      } catch (e) {
        // 错误已在 request 拦截器提示
      } finally {
        this.submitting = false
      }
    },

    openImport() {
      this.importForm = { category: '' }
      this.fileList = []
      this.selectedFile = null
      this.importVisible = true
    },

    onFileChange(file) {
      this.selectedFile = file.raw
    },

    onFileRemove() {
      this.selectedFile = null
    },

    async submitImport() {
      if (!this.selectedFile) return Message.warning('请选择文件')
      this.submitting = true
      try {
        await importKnowledgeApi(this.selectedFile, this.importForm.category)
        Message.success('导入成功')
        this.importVisible = false
        this.loadList()
      } catch (e) {
        // 错误已在 request 拦截器提示
      } finally {
        this.submitting = false
      }
    },

    remove(row) {
      this.$confirm(`确定删除知识「${row.title}」吗？`, '删除确认', { type: 'warning' })
        .then(async () => {
          try {
            await deleteKnowledgeApi(row.title)
            Message.success('删除成功')
            this.loadList()
          } catch (e) {
            // 错误已在 request 拦截器提示
          }
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.knowledge-container { padding: 10px; }
.toolbar-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 16px; font-weight: 600; color: #303133; }
.danger-text { color: #f56c6c; }
.upload-tip { margin-left: 12px; color: #909399; font-size: 12px; }
</style>
