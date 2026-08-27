<template>
  <el-dialog
    :title="groupDialogTitle"
    :visible="dialogVisible"
    width="420px"
    @close="resetGroupDialog"
  >
    <el-form :model="groupForm" :rules="groupRules" ref="groupForm" label-width="90px">
      <el-form-item label="范围组名称" prop="groupName">
        <el-input v-model="groupForm.groupName" placeholder="例：海秀支行" clearable />
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="groupSubmitting" @click="submitGroupForm">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { Message } from 'element-ui'
import { createGroupApi, updateGroupApi } from '@/api/org'

export default {
  name: 'GroupDialog',
  props: {
    visible: { type: Boolean, default: false },
    // 编辑时传入范围组对象，新建时传 null
    group: { type: Object, default: null }
  },
  data () {
    return {
      groupForm: { groupName: '' },
      groupRules: {
        groupName: [{ required: true, message: '请输入范围组名称', trigger: 'blur' }]
      },
      groupSubmitting: false
    }
  },
  computed: {
    dialogVisible: {
      get () { return this.visible },
      set (val) { this.$emit('update:visible', val) }
    },
    isGroupEdit () { return !!(this.group && this.group.groupCode) },
    groupDialogTitle () { return this.isGroupEdit ? '编辑范围组' : '新建范围组' }
  },
  watch: {
    visible (val) {
      if (val) {
        this.groupForm.groupName = this.group ? this.group.groupName : ''
      }
    }
  },
  methods: {
    close () {
      this.$emit('update:visible', false)
    },
    async submitGroupForm () {
      const valid = await this.$refs.groupForm.validate().catch(() => false)
      if (!valid) return
      this.groupSubmitting = true
      try {
        if (this.isGroupEdit) {
          await updateGroupApi(this.group.groupCode, this.groupForm.groupName.trim())
          Message.success('更新范围组成功')
        } else {
          await createGroupApi(this.groupForm.groupName.trim())
          Message.success('新建范围组成功')
        }
        this.$emit('update:visible', false)
        this.$emit('success')
      } catch (e) {
        Message.error('操作失败：' + (e.message || '未知错误'))
      } finally {
        this.groupSubmitting = false
      }
    },
    resetGroupDialog () {
      this.groupForm = { groupName: '' }
      this.$refs.groupForm && this.$refs.groupForm.resetFields()
    }
  }
}
</script>
