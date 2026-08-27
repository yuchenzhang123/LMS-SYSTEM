<template>
  <el-dialog
    title="添加管理人员"
    :visible="dialogVisible"
    width="480px"
    @close="resetManagerDialog"
  >
    <el-form :model="managerForm" :rules="managerRules" ref="managerForm" label-width="90px">
      <el-form-item label="EHR号" prop="ehrNo">
        <el-input v-model="managerForm.ehrNo" placeholder="请输入人员EHR号" clearable @blur="onManagerEhrBlur">
          <el-button slot="append" icon="el-icon-search" :loading="managerLookupLoading" @click="onManagerEhrBlur">查询</el-button>
        </el-input>
        <transition name="lookup-fade">
          <div v-if="managerLookupResult" class="lookup-hint" :class="managerLookupResult.found ? 'hint-found' : 'hint-notfound'">
            <i :class="managerLookupResult.found ? 'el-icon-circle-check' : 'el-icon-circle-close'" />
            {{ managerLookupResult.found
                ? `找到：${managerLookupResult.userName || ''} (${managerLookupResult.orgCode || ''})`
                : '未找到该EHR号对应的人员信息' }}
          </div>
        </transition>
      </el-form-item>
      <el-form-item label="姓名" prop="userName">
        <el-input v-model="managerForm.userName" placeholder="请输入人员姓名" clearable />
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="managerSubmitting" @click="submitManagerForm">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { Message, MessageBox } from 'element-ui'
import { addGroupManagerApi, userLookupApi } from '@/api/org'

export default {
  name: 'ManagerDialog',
  props: {
    visible: { type: Boolean, default: false },
    groupCode: { type: String, default: '' }
  },
  data () {
    return {
      managerForm: { ehrNo: '', userName: '' },
      managerRules: {
        ehrNo: [{ required: true, message: '请输入EHR号', trigger: 'blur' }],
        userName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
      },
      managerSubmitting: false,
      managerLookupLoading: false,
      managerLookupResult: null
    }
  },
  computed: {
    dialogVisible: {
      get () { return this.visible },
      set (val) { this.$emit('update:visible', val) }
    }
  },
  methods: {
    close () {
      this.$emit('update:visible', false)
    },
    async onManagerEhrBlur () {
      const ehrNo = this.managerForm.ehrNo.trim()
      if (!ehrNo) { this.managerLookupResult = null; return }
      this.managerLookupResult = null
      this.managerLookupLoading = true
      try {
        const res = await userLookupApi(ehrNo)
        this.managerLookupResult = res.data || res
        if (this.managerLookupResult && this.managerLookupResult.found) {
          if (this.managerLookupResult.userName && !this.managerForm.userName) {
            this.managerForm.userName = this.managerLookupResult.userName
          }
        }
      } catch (e) {
        this.managerLookupResult = { found: false }
      } finally {
        this.managerLookupLoading = false
      }
    },
    async submitManagerForm () {
      const valid = await this.$refs.managerForm.validate().catch(() => false)
      if (!valid) return
      if (this.managerLookupResult && !this.managerLookupResult.found) {
        try {
          await MessageBox.confirm(
            `未找到EHR号「${this.managerForm.ehrNo.trim()}」对应的人员信息，确定继续添加吗？`,
            '确认操作',
            { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }
          )
        } catch (e) { return }
      }
      this.managerSubmitting = true
      try {
        await addGroupManagerApi(
          this.groupCode,
          this.managerForm.ehrNo.trim(),
          this.managerForm.userName.trim()
        )
        Message.success('添加管理人员成功')
        this.$emit('update:visible', false)
        this.$emit('success')
      } catch (e) {
        Message.error('添加失败：' + (e.message || '未知错误'))
      } finally {
        this.managerSubmitting = false
      }
    },
    resetManagerDialog () {
      this.managerForm = { ehrNo: '', userName: '' }
      this.managerLookupResult = null
      this.$refs.managerForm && this.$refs.managerForm.resetFields()
    }
  }
}
</script>

<style scoped>
.lookup-hint { font-size: 12px; margin-top: 4px; line-height: 1.4; }
.hint-found { color: #67C23A; }
.hint-notfound { color: #F56C6C; }
.lookup-fade-enter-active { animation: lookup-pop 0.2s ease-out; }
@keyframes lookup-pop {
  from { opacity: 0; transform: translateY(-4px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
