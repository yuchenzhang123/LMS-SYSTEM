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
          <el-form-item label="提交律所时间" v-if="isFieldVisible('submitToLawFirmDate')">
            <el-date-picker v-model="form.submitToLawFirmDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择提交律所时间" style="width: 100%;"></el-date-picker>
          </el-form-item>
          <el-form-item label="律所名称" v-if="isFieldVisible('lawFirm')">
            <el-input v-model="form.lawFirm" placeholder="请输入律所名称"></el-input>
          </el-form-item>
          <el-form-item label="提交法院时间" v-if="isFieldVisible('submitToCourtDate')">
            <el-date-picker v-model="form.submitToCourtDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择提交法院时间" style="width: 100%;"></el-date-picker>
          </el-form-item>
          <el-form-item label="涉及法院" v-if="isFieldVisible('courtName')">
            <el-input v-model="form.courtName" placeholder="请输入涉及法院"></el-input>
          </el-form-item>
          <el-form-item label="诉讼立案案号" v-if="isFieldVisible('filingCaseNo')">
            <el-input v-model="form.filingCaseNo" placeholder="请输入诉讼立案案号"></el-input>
          </el-form-item>
          <el-form-item label="是否开庭" v-if="isFieldVisible('isHearing')">
            <el-switch v-model="form.isHearing" :active-value="true" :inactive-value="false" active-text="是" inactive-text="否"></el-switch>
          </el-form-item>
          <el-form-item label="开庭时间" v-if="isFieldVisible('hearingDate')">
            <el-date-picker v-model="form.hearingDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择开庭时间" style="width: 100%;"></el-date-picker>
          </el-form-item>
          <el-form-item label="判决时间" v-if="isFieldVisible('judgmentDate')">
            <el-date-picker v-model="form.judgmentDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择判决时间" style="width: 100%;"></el-date-picker>
          </el-form-item>
          <el-form-item label="执行申请提交时间" v-if="isFieldVisible('executionApplyToCourtDate')">
            <el-date-picker v-model="form.executionApplyToCourtDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择执行申请提交时间" style="width: 100%;"></el-date-picker>
          </el-form-item>
          <el-form-item label="执行立案时间" v-if="isFieldVisible('executionFilingDate')">
            <el-date-picker v-model="form.executionFilingDate" type="date" value-format="yyyy-MM-dd" placeholder="请选择执行立案时间" style="width: 100%;"></el-date-picker>
          </el-form-item>
          <el-form-item label="执行立案案号" v-if="isFieldVisible('executionCaseNo')">
            <el-input v-model="form.executionCaseNo" placeholder="请输入执行立案案号"></el-input>
          </el-form-item>
          <el-form-item label="拍卖状态" v-if="isFieldVisible('auctionStatus')">
            <el-input v-model="form.auctionStatus" placeholder="例如：一拍、二拍、变卖流拍"></el-input>
          </el-form-item>
          <el-form-item label="诉讼费" v-if="isFieldVisible('litigationFee')">
            <el-input v-model="form.litigationFee" placeholder="请输入诉讼费金额"><template slot="append">元</template></el-input>
          </el-form-item>
          <el-form-item v-if="isFieldVisible('litigationFee')">
            <el-checkbox v-model="form.litigationFeePaidByCustomer">客户已支付</el-checkbox>
          </el-form-item>
          <el-form-item label="保全费" v-if="isFieldVisible('preservationFee')">
            <el-input v-model="form.preservationFee" placeholder="请输入保全费金额"><template slot="append">元</template></el-input>
          </el-form-item>
          <el-form-item v-if="isFieldVisible('preservationFee')">
            <el-checkbox v-model="form.preservationFeePaidByCustomer">客户已支付</el-checkbox>
          </el-form-item>
          <el-form-item label="评估费" v-if="isFieldVisible('appraisalFee')">
            <el-input v-model="form.appraisalFee" placeholder="请输入评估费金额"><template slot="append">元</template></el-input>
          </el-form-item>
          <el-form-item label="诉讼和保全支付时间" v-if="isFieldVisible('litigationPreservationPaidAt')">
            <el-date-picker v-model="form.litigationPreservationPaidAt" type="date" value-format="yyyy-MM-dd" placeholder="请选择诉讼和保全支付时间" style="width: 100%;"></el-date-picker>
          </el-form-item>
          <el-form-item label="诉讼和保全销账时间" v-if="isFieldVisible('litigationPreservationWriteOffAt')">
            <el-date-picker v-model="form.litigationPreservationWriteOffAt" type="date" value-format="yyyy-MM-dd" placeholder="请选择诉讼和保全销账时间" style="width: 100%;"></el-date-picker>
          </el-form-item>
          <el-form-item label="律师费" v-if="isFieldVisible('lawyerFee')">
            <el-input v-model="form.lawyerFee" placeholder="请输入律师费金额"><template slot="append">元</template></el-input>
          </el-form-item>
          <el-form-item v-if="isFieldVisible('lawyerFee')">
            <el-checkbox v-model="form.lawyerFeePaidByCustomer">客户已支付</el-checkbox>
          </el-form-item>
          <el-form-item label="进度备注">
            <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入诉讼进度备注"></el-input>
          </el-form-item>
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
import { MessageBox, Message } from 'element-ui'

// 字段元信息：key, label, 所属阶段(用于回退清空判断)
const ALL_FIELDS = [
  { key: 'submitToLawFirmDate', label: '提交律所时间' },
  { key: 'lawFirm', label: '律所名称' },
  { key: 'submitToCourtDate', label: '提交法院时间' },
  { key: 'courtName', label: '涉及法院' },
  { key: 'filingCaseNo', label: '诉讼立案案号' },
  { key: 'isHearing', label: '是否开庭' },
  { key: 'hearingDate', label: '开庭时间' },
  { key: 'judgmentDate', label: '判决时间' },
  { key: 'executionApplyToCourtDate', label: '执行申请提交时间' },
  { key: 'executionFilingDate', label: '执行立案时间' },
  { key: 'executionCaseNo', label: '执行立案案号' },
  { key: 'auctionStatus', label: '拍卖状态' },
  { key: 'litigationFee', label: '诉讼费' },
  { key: 'preservationFee', label: '保全费' },
  { key: 'appraisalFee', label: '评估费' },
  { key: 'litigationPreservationPaidAt', label: '诉讼和保全支付时间' },
  { key: 'litigationPreservationWriteOffAt', label: '诉讼和保全销账时间' },
  { key: 'lawyerFee', label: '律师费' }
]

const STRING_FIELDS = [
  'submitToLawFirmDate', 'lawFirm', 'submitToCourtDate', 'courtName', 'filingCaseNo',
  'hearingDate', 'judgmentDate', 'executionApplyToCourtDate', 'executionFilingDate',
  'executionCaseNo', 'auctionStatus', 'litigationPreservationPaidAt', 'litigationPreservationWriteOffAt'
]

const BOOL_FIELDS = [
  'litigationFeePaidByCustomer', 'preservationFeePaidByCustomer', 'lawyerFeePaidByCustomer'
]

export default {
  name: 'LitigationDialog',
  props: {
    visible: { type: Boolean, default: false },
    isEditMode: { type: Boolean, default: false },
    submitLoading: { type: Boolean, default: false },
    currentLitigation: { type: Object, default: () => ({}) },
    litigationForm: { type: Object, default: () => ({}) },
    litigationStatusOptions: { type: Array, default: () => [] }
  },
  data () {
    return {
      _prevStatus: '',   // 切换前的状态码
      _initialStatus: '' // 进入编辑时的状态码（用于保存时判断回退）
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
        this._prevStatus = this.litigationForm.statusCode || ''
        this._initialStatus = this._prevStatus
      }
    }
  },
  methods: {
    getFieldKeysByStatus (statusCode) {
      const code = String(statusCode || '')
      if (!code) return []
      const keys = ['submitToLawFirmDate', 'lawFirm']
      if (code.startsWith('2.') || code.startsWith('3.')) {
        keys.push('submitToCourtDate', 'courtName', 'filingCaseNo')
      }
      if (['2.3', '3.1', '3.2', '3.3', '3.3.1', '3.3.2', '3.4', '3.5', '3.6', '3.8', '3.9', '3.9.2'].includes(code)) {
        keys.push('isHearing', 'hearingDate')
      }
      if (code.startsWith('3.')) {
        keys.push('judgmentDate', 'executionApplyToCourtDate', 'executionFilingDate', 'executionCaseNo')
      }
      if (['3.3.1', '3.4'].includes(code)) {
        keys.push('auctionStatus')
      }
      const statusMeta = this.litigationStatusOptions.find(item => item.code === code)
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
      const oldKeys = this.getFieldKeysByStatus(this._prevStatus)
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

      this._prevStatus = newCode || ''
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
      const currentCode = String(this._initialStatus || '')
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
