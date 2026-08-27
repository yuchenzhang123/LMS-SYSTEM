<template>
  <el-dialog
    title="添加机构"
    :visible="dialogVisible"
    width="450px"
    @close="resetMemberDialog"
  >
    <el-form :model="memberForm" :rules="memberRules" ref="memberForm" label-width="90px">
      <el-form-item label="机构号" prop="orgCode">
        <el-input v-model="memberForm.orgCode" placeholder="请输入机构号" clearable @blur="onMemberCodeBlur">
          <el-button slot="append" icon="el-icon-search" :loading="memberLookupLoading" @click="onMemberCodeBlur">查询</el-button>
        </el-input>
        <transition name="lookup-fade">
          <div v-if="memberLookupResult" class="lookup-hint" :class="memberLookupResult.found ? 'hint-found' : 'hint-notfound'">
            <i :class="memberLookupResult.found ? 'el-icon-circle-check' : 'el-icon-circle-close'" />
            {{ memberLookupResult.found ? `找到：${memberLookupResult.orgName}` : '未找到该机构号' }}
          </div>
        </transition>
      </el-form-item>
      <el-form-item label="机构名称" prop="orgName">
        <el-input v-model="memberForm.orgName" placeholder="请输入机构名称" clearable />
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="memberSubmitting" @click="submitMemberForm">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { Message, MessageBox } from 'element-ui'
import { addMemberApi, lookupOrgInGbaseApi } from '@/api/org'

export default {
  name: 'MemberDialog',
  props: {
    visible: { type: Boolean, default: false },
    groupCode: { type: String, default: '' }
  },
  data () {
    return {
      memberForm: { orgCode: '', orgName: '' },
      memberRules: {
        orgCode: [{ required: true, message: '请输入机构号', trigger: 'blur' }],
        orgName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }]
      },
      memberSubmitting: false,
      memberLookupLoading: false,
      memberLookupResult: null
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
    async onMemberCodeBlur () {
      const code = this.memberForm.orgCode.trim()
      if (!code) { this.memberLookupResult = null; return }
      this.memberLookupResult = null
      this.memberLookupLoading = true
      try {
        const res = await lookupOrgInGbaseApi(code)
        this.memberLookupResult = res.data || res
        if (this.memberLookupResult && this.memberLookupResult.found && this.memberLookupResult.orgName && !this.memberForm.orgName) {
          this.memberForm.orgName = this.memberLookupResult.orgName
        }
      } catch (e) {
        this.memberLookupResult = { found: false }
      } finally {
        this.memberLookupLoading = false
      }
    },
    async submitMemberForm () {
      const valid = await this.$refs.memberForm.validate().catch(() => false)
      if (!valid) return
      if (this.memberLookupResult && !this.memberLookupResult.found) {
        try {
          await MessageBox.confirm(
            `未找到机构号「${this.memberForm.orgCode.trim()}」，确定继续添加吗？`,
            '确认操作',
            { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }
          )
        } catch (e) { return }
      }
      this.memberSubmitting = true
      try {
        await addMemberApi(this.groupCode, this.memberForm.orgCode.trim(), this.memberForm.orgName.trim())
        Message.success('添加机构成功')
        this.$emit('update:visible', false)
        this.$emit('success')
      } catch (e) {
        Message.error('添加失败：' + (e.message || '未知错误'))
      } finally {
        this.memberSubmitting = false
      }
    },
    resetMemberDialog () {
      this.memberForm = { orgCode: '', orgName: '' }
      this.memberLookupResult = null
      this.$refs.memberForm && this.$refs.memberForm.resetFields()
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
