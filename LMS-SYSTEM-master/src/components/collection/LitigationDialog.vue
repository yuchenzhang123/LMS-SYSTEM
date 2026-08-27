<template>
  <el-dialog
    :title="dialogTitle"
    :visible="dialogVisible"
    width="800px"
    @close="handleClose"
  >
    <div class="dialog-content" :class="{ 'edit-mode': isEditMode }">
      <!-- View mode toolbar -->
      <div class="dialog-toolbar" v-if="!isEditMode">
        <el-button type="primary" size="small" @click="$emit('toggleEditMode')">
          登记诉讼进度
        </el-button>
      </div>

      <!-- 已保存信息 -->
      <el-descriptions
        :title="isEditMode ? '当前已保存信息' : '诉讼详情'"
        :column="2"
        border
        :size="isEditMode ? 'mini' : undefined"
      >
        <el-descriptions-item label="诉讼状态">
          <el-tag :type="currentLitigation.inLitigation ? 'warning' : 'success'">
            {{ currentLitigation.statusText || '未登记' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="诉讼ID">{{ currentLitigation.litigationId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentLitigation.createdAt || '--' }}</el-descriptions-item>
        <el-descriptions-item label="最近更新">{{ currentLitigation.updatedAt || '--' }}</el-descriptions-item>
        <template v-for="field in savedFieldList">
          <el-descriptions-item :key="field.key" :label="field.label">
            <span v-if="field.key === 'isHearing'">{{ currentLitigation.isHearing ? '是' : '否' }}</span>
            <span v-else-if="['litigationFee', 'preservationFee', 'lawyerFee'].includes(field.key)"
                  :class="{ 'danger-text': currentLitigation[field.key + 'PaidByCustomer'] }">
              {{ currentLitigation[field.key] || '--' }}
            </span>
            <span v-else>{{ currentLitigation[field.key] || '--' }}</span>
          </el-descriptions-item>
        </template>
        <el-descriptions-item label="诉讼备注" :span="2">{{ currentLitigation.remark || '--' }}</el-descriptions-item>
      </el-descriptions>

      <!-- Edit form -->
      <template v-if="isEditMode">
        <el-divider>修改内容</el-divider>
        <el-form :model="form" label-width="110px">
          <el-form-item label="诉讼状态">
            <el-select v-model="form.statusCode" filterable style="width: 100%;" placeholder="请选择诉讼状态" @change="onStatusChange">
              <el-option
                v-for="item in litigationStatusOptions"
                :key="item.code"
                :label="`${item.code} ${item.label}`"
                :value="item.code"
              ></el-option>
            </el-select>
          </el-form-item>

          <template v-for="f in editFields">
            <template v-if="isFieldVisible(f.key)">
              <el-form-item :key="f.key" :label="f.label">
                <el-date-picker
                  v-if="f.type === 'date'"
                  v-model="form[f.key]"
                  type="date"
                  value-format="yyyy-MM-dd"
                  :placeholder="f.placeholder"
                  style="width: 100%;"
                ></el-date-picker>
                <el-input v-else-if="f.type === 'text'" v-model="form[f.key]" :placeholder="f.placeholder"></el-input>
                <el-switch
                  v-else-if="f.type === 'switch'"
                  v-model="form[f.key]"
                  :active-value="true"
                  :inactive-value="false"
                  active-text="是"
                  inactive-text="否"
                ></el-switch>
                <el-input v-else-if="f.type === 'money'" v-model="form[f.key]" :placeholder="f.placeholder">
                  <template slot="append">元</template>
                </el-input>
                <el-input
                  v-else-if="f.type === 'textarea'"
                  v-model="form[f.key]"
                  type="textarea"
                  :rows="3"
                  :placeholder="f.placeholder"
                ></el-input>
              </el-form-item>
              <el-form-item v-if="f.type === 'money' && f.paidKey" :key="f.key + '_paid'">
                <el-checkbox v-model="form[f.paidKey]">客户已支付</el-checkbox>
              </el-form-item>
            </template>
          </template>
        </el-form>
      </template>
    </div>
    <span slot="footer" v-if="isEditMode">
      <el-button @click="$emit('cancelEdit')">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确认保存</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { MessageBox } from 'element-ui'
import {
  LITIGATION_STATUS_OPTIONS,
  ALL_FIELDS,
  STRING_FIELDS,
  BOOL_FIELDS,
  EDIT_FIELDS
} from '@/utils/litigation'

export default {
  name: 'LitigationDialog',
  props: {
    visible: { type: Boolean, default: false },
    isEditMode: { type: Boolean, default: false },
    submitLoading: { type: Boolean, default: false },
    currentLitigation: { type: Object, default: () => ({}) },
    litigationForm: { type: Object, default: () => ({}) }
  },
  data () {
    return {
      litigationStatusOptions: LITIGATION_STATUS_OPTIONS,
      editFields: EDIT_FIELDS,
      prevStatus: '',   // 切换前的状态码
      initialStatus: '' // 进入编辑时的状态码（用于保存时判断回退）
    }
  },
  computed: {
    dialogVisible: {
      get () { return this.visible },
      set (val) { this.$emit('update:visible', val) }
    },
    dialogTitle () {
      return this.isEditMode ? '登记诉讼进度' : '诉讼详情'
    },
    displayFieldKeys () {
      return this.getFieldKeysByStatus(this.currentLitigation.statusCode)
    },
    // 编辑模式下展示所有已填字段，不受状态限制
    savedFieldList () {
      if (!this.isEditMode) {
        return ALL_FIELDS.filter(f => this.displayFieldKeys.includes(f.key))
      }
      return ALL_FIELDS.filter(f => {
        const val = this.currentLitigation[f.key]
        return val !== null && val !== undefined && val !== '' && val !== false && val !== '0' && val !== '0.00'
      })
    },
    form () {
      return this.litigationForm
    }
  },
  watch: {
    visible (val) {
      if (val) {
        this.prevStatus = this.litigationForm.statusCode || ''
        this.initialStatus = this.prevStatus
      }
    }
  },
  methods: {
    getFieldKeysByStatus (statusCode) {
      const code = String(statusCode || '')
      if (!code) return []
      const keys = ['submitToLawFirmDate', 'lawFirm']

      // 法院信息：2.x/3.x 及 4.1/4.2/4.3 可见（经过法院流程）
      if (code.startsWith('2.') || code.startsWith('3.') || code === '4.1' || code === '4.2' || code === '4.3') {
        keys.push('submitToCourtDate', 'courtName', 'filingDate', 'filingCaseNo')
      }

      // 开庭信息
      const hearingCodes = ['2.3', '3.1', '3.2', '3.3', '3.3.1', '3.3.2', '3.4', '3.5', '3.6', '3.8', '3.9', '4.1', '4.2', '4.3']
      if (hearingCodes.includes(code)) {
        keys.push('isHearing', 'hearingDate')
      }

      // 判决/执行：3.x 及 4.1/4.2/4.3 可见
      if (code.startsWith('3.') || code === '4.1' || code === '4.2' || code === '4.3') {
        keys.push('judgmentDate', 'executionApplyToCourtDate', 'executionFilingDate', 'executionCaseNo')
      }

      // 拍卖状态
      if (['3.3.1', '3.4'].includes(code)) {
        keys.push('auctionStatus')
      }

      // 费用：inLitigation=true 可见
      const statusMeta = LITIGATION_STATUS_OPTIONS.find(item => item.code === code)
      if (statusMeta && statusMeta.inLitigation) {
        keys.push('litigationFee', 'preservationFee', 'appraisalFee', 'litigationPreservationPaidAt', 'litigationPreservationWriteOffAt', 'lawyerFee')
      }

      return [...new Set(keys)]
    },
    isFieldVisible (fieldKey) {
      const code = String(this.litigationForm.statusCode || '')
      const currentCode = String(this.currentLitigation.statusCode || '')
      if (code === currentCode) {
        return this.displayFieldKeys.includes(fieldKey)
      }
      return this.getFieldKeysByStatus(code).includes(fieldKey)
    },
    // 切换状态时自动恢复已存值 / 清空不可见字段，无弹窗干扰
    onStatusChange (newCode) {
      const oldKeys = this.getFieldKeysByStatus(this.prevStatus)
      const newKeys = this.getFieldKeysByStatus(newCode || '')
      const added = newKeys.filter(k => !oldKeys.includes(k))
      const removed = oldKeys.filter(k => !newKeys.includes(k))

      // 新增可见字段：从已保存记录恢复值
      for (const key of added) {
        const saved = this.currentLitigation[key]
        if (saved === undefined || saved === null) continue
        if (STRING_FIELDS.includes(key) && saved !== '') {
          this.form[key] = saved
        } else if (key === 'isHearing') {
          this.form[key] = !!saved
        } else if (['litigationFee', 'preservationFee', 'appraisalFee', 'lawyerFee'].includes(key) && saved !== '0' && saved !== '0.00') {
          this.form[key] = saved
        } else if (BOOL_FIELDS.includes(key)) {
          this.form[key] = !!saved
        }
      }

      // 不再可见字段：清空
      for (const key of removed) {
        this.clearField(key)
      }

      this.prevStatus = newCode || ''
    },
    clearField (key) {
      if (STRING_FIELDS.includes(key)) {
        this.form[key] = ''
      } else if (BOOL_FIELDS.includes(key) || key === 'isHearing') {
        this.form[key] = false
      } else {
        this.form[key] = ''
      }
    },
    clearFields (fieldKeys) {
      for (const key of fieldKeys) this.clearField(key)
    },
    // 仅保存时判断回退并提示
    handleSubmit () {
      const currentCode = String(this.initialStatus || '')
      const newCode = String(this.litigationForm.statusCode || '')
      const isBackward = currentCode && newCode && newCode < currentCode

      const newKeys = this.getFieldKeysByStatus(newCode)
      const allFieldKeys = ALL_FIELDS.map(f => f.key)
      const toRemove = allFieldKeys.filter(k => !newKeys.includes(k))
      const hasInvisibleData = toRemove.some(k => {
        const v = this.form[k]
        return v !== null && v !== undefined && v !== '' && v !== false && v !== '0' && v !== '0.00'
      })

      if (isBackward && hasInvisibleData) {
        MessageBox.confirm(
          '当前更改涉及诉讼状态回退，后续阶段的已填字段将在保存后清空，确定继续吗？',
          '状态回退确认',
          { type: 'warning', confirmButtonText: '确定保存', cancelButtonText: '取消' }
        ).then(() => {
          this.$emit('submitProgress')
        }).catch(() => {})
        return
      }
      this.$emit('submitProgress')
    },
    handleClose () {
      this.$emit('update:visible', false)
      this.$emit('close')
    }
  }
}
</script>

<style scoped>
.dialog-content { padding: 0; }
.dialog-content.edit-mode { max-height: 70vh; overflow-y: auto; padding-right: 4px; }
.dialog-toolbar { display: flex; gap: 10px; margin-bottom: 20px; }
.danger-text { color: #f56c6c; font-weight: bold; }
</style>
