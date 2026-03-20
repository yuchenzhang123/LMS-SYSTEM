<template>
  <el-dialog
    :title="dialogTitle"
    :visible="dialogVisible"
    width="800px"
    @close="handleClose"
  >
    <div class="dialog-content">
      <div class="dialog-toolbar" v-if="!isEditMode">
        <el-button type="primary" size="small" @click="$emit('toggleEditMode')">
          登记诉讼进度
        </el-button>
      </div>

      <el-descriptions v-if="!isEditMode" title="诉讼详情" :column="2" border>
        <el-descriptions-item label="诉讼状态">
          <el-tag :type="currentLitigation.inLitigation ? 'warning' : 'success'">
            {{ currentLitigation.statusText || '未登记' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="诉讼ID">{{ currentLitigation.litigationId || '--' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentLitigation.createdAt || '--' }}</el-descriptions-item>
        <el-descriptions-item label="最近更新">{{ currentLitigation.updatedAt || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('submitToLawFirmDate')" label="提交律所时间">{{ currentLitigation.submitToLawFirmDate || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('lawFirm')" label="律所名称">{{ currentLitigation.lawFirm || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('submitToCourtDate')" label="提交法院时间">{{ currentLitigation.submitToCourtDate || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('courtName')" label="涉及法院">{{ currentLitigation.courtName || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('filingCaseNo')" label="诉讼立案案号">{{ currentLitigation.filingCaseNo || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('isHearing')" label="是否开庭">{{ currentLitigation.isHearing ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('hearingDate')" label="开庭时间">{{ currentLitigation.hearingDate || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('judgmentDate')" label="判决时间">{{ currentLitigation.judgmentDate || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('executionApplyToCourtDate')" label="执行申请提交时间">{{ currentLitigation.executionApplyToCourtDate || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('executionFilingDate')" label="执行立案时间">{{ currentLitigation.executionFilingDate || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('executionCaseNo')" label="执行立案案号">{{ currentLitigation.executionCaseNo || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('auctionStatus')" label="拍卖状态">{{ currentLitigation.auctionStatus || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('litigationFee')" label="诉讼费">
          <span :class="{ 'danger-text': currentLitigation.litigationFeePaidByCustomer }">{{ currentLitigation.litigationFee || '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('preservationFee')" label="保全费">
          <span :class="{ 'danger-text': currentLitigation.preservationFeePaidByCustomer }">{{ currentLitigation.preservationFee || '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('appraisalFee')" label="评估费">{{ currentLitigation.appraisalFee || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('litigationPreservationPaidAt')" label="诉讼和保全支付时间">{{ currentLitigation.litigationPreservationPaidAt || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('litigationPreservationWriteOffAt')" label="诉讼和保全销账时间">{{ currentLitigation.litigationPreservationWriteOffAt || '--' }}</el-descriptions-item>
        <el-descriptions-item v-if="displayFieldKeys.includes('lawyerFee')" label="律师费">
          <span :class="{ 'danger-text': currentLitigation.lawyerFeePaidByCustomer }">{{ currentLitigation.lawyerFee || '--' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="诉讼备注" :span="2">{{ currentLitigation.remark || '--' }}</el-descriptions-item>
      </el-descriptions>

      <el-form v-if="isEditMode" :model="form" label-width="110px">
        <el-form-item label="诉讼状态">
          <el-select v-model="form.statusCode" filterable style="width: 100%;" placeholder="请选择诉讼状态">
            <el-option
              v-for="item in litigationStatusOptions"
              :key="item.code"
              :label="`${item.code} ${item.label}`"
              :value="item.code"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="提交律所时间" v-if="isFieldVisible('submitToLawFirmDate')">
          <el-date-picker
            v-model="form.submitToLawFirmDate"
            type="date"
            value-format="yyyy/M/d"
            placeholder="请选择提交律所时间"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="律所名称" v-if="isFieldVisible('lawFirm')">
          <el-input v-model="form.lawFirm" placeholder="请输入律所名称"></el-input>
        </el-form-item>
        <el-form-item label="提交法院时间" v-if="isFieldVisible('submitToCourtDate')">
          <el-date-picker
            v-model="form.submitToCourtDate"
            type="date"
            value-format="yyyy/M/d"
            placeholder="请选择提交法院时间"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="涉及法院" v-if="isFieldVisible('courtName')">
          <el-input v-model="form.courtName" placeholder="请输入涉及法院"></el-input>
        </el-form-item>
        <el-form-item label="诉讼立案案号" v-if="isFieldVisible('filingCaseNo')">
          <el-input v-model="form.filingCaseNo" placeholder="请输入诉讼立案案号"></el-input>
        </el-form-item>
        <el-form-item label="是否开庭" v-if="isFieldVisible('isHearing')">
          <el-switch
            v-model="form.isHearing"
            :active-value="true"
            :inactive-value="false"
            active-text="是"
            inactive-text="否"
          ></el-switch>
        </el-form-item>
        <el-form-item label="开庭时间" v-if="isFieldVisible('hearingDate')">
          <el-date-picker
            v-model="form.hearingDate"
            type="date"
            value-format="yyyy/M/d"
            placeholder="请选择开庭时间"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="判决时间" v-if="isFieldVisible('judgmentDate')">
          <el-date-picker
            v-model="form.judgmentDate"
            type="date"
            value-format="yyyy/M/d"
            placeholder="请选择判决时间"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="执行申请提交时间" v-if="isFieldVisible('executionApplyToCourtDate')">
          <el-date-picker
            v-model="form.executionApplyToCourtDate"
            type="date"
            value-format="yyyy/M/d"
            placeholder="请选择执行申请提交时间"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="执行立案时间" v-if="isFieldVisible('executionFilingDate')">
          <el-date-picker
            v-model="form.executionFilingDate"
            type="date"
            value-format="yyyy/M/d"
            placeholder="请选择执行立案时间"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="执行立案案号" v-if="isFieldVisible('executionCaseNo')">
          <el-input v-model="form.executionCaseNo" placeholder="请输入执行立案案号"></el-input>
        </el-form-item>
        <el-form-item label="拍卖状态" v-if="isFieldVisible('auctionStatus')">
          <el-input v-model="form.auctionStatus" placeholder="例如：一拍、二拍、变卖流拍"></el-input>
        </el-form-item>
        <el-form-item label="诉讼费" v-if="isFieldVisible('litigationFee')">
          <el-input v-model="form.litigationFee" placeholder="请输入诉讼费金额">
            <template slot="append">元</template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="isFieldVisible('litigationFee')">
          <el-checkbox v-model="form.litigationFeePaidByCustomer">客户已支付</el-checkbox>
        </el-form-item>
        <el-form-item label="保全费" v-if="isFieldVisible('preservationFee')">
          <el-input v-model="form.preservationFee" placeholder="请输入保全费金额">
            <template slot="append">元</template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="isFieldVisible('preservationFee')">
          <el-checkbox v-model="form.preservationFeePaidByCustomer">客户已支付</el-checkbox>
        </el-form-item>
        <el-form-item label="评估费" v-if="isFieldVisible('appraisalFee')">
          <el-input v-model="form.appraisalFee" placeholder="请输入评估费金额">
            <template slot="append">元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="诉讼和保全支付时间" v-if="isFieldVisible('litigationPreservationPaidAt')">
          <el-date-picker
            v-model="form.litigationPreservationPaidAt"
            type="date"
            value-format="yyyy/M/d"
            placeholder="请选择诉讼和保全支付时间"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="诉讼和保全销账时间" v-if="isFieldVisible('litigationPreservationWriteOffAt')">
          <el-date-picker
            v-model="form.litigationPreservationWriteOffAt"
            type="date"
            value-format="yyyy/M/d"
            placeholder="请选择诉讼和保全销账时间"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
        <el-form-item label="律师费" v-if="isFieldVisible('lawyerFee')">
          <el-input v-model="form.lawyerFee" placeholder="请输入律师费金额">
            <template slot="append">元</template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="isFieldVisible('lawyerFee')">
          <el-checkbox v-model="form.lawyerFeePaidByCustomer">客户已支付</el-checkbox>
        </el-form-item>
        <el-form-item label="进度备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入诉讼进度备注"
          ></el-input>
        </el-form-item>
      </el-form>
    </div>
    <span slot="footer" v-if="isEditMode">
      <el-button @click="$emit('cancelEdit')">取消</el-button>
      <el-button type="primary" @click="$emit('submitProgress')" :loading="submitLoading">确认保存</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: 'LitigationDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    isEditMode: {
      type: Boolean,
      default: false
    },
    submitLoading: {
      type: Boolean,
      default: false
    },
    currentLitigation: {
      type: Object,
      default: () => ({})
    },
    litigationForm: {
      type: Object,
      default: () => ({})
    },
    litigationStatusOptions: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    dialogVisible: {
      get () {
        return this.visible
      },
      set (val) {
        this.$emit('update:visible', val)
      }
    },
    dialogTitle () {
      return this.isEditMode ? '登记诉讼进度' : '诉讼详情'
    },
    displayFieldKeys () {
      return this.getFieldKeysByStatus(this.currentLitigation.statusCode)
    },
    form () {
      return this.litigationForm
    }
  },
  methods: {
    getFieldKeysByStatus (statusCode) {
      const code = String(statusCode || '')
      if (!code) {
        return []
      }
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
      return this.getFieldKeysByStatus(this.litigationForm.statusCode).includes(fieldKey)
    },
    handleClose () {
      this.$emit('update:visible', false)
      this.$emit('close')
    }
  }
}
</script>

<style scoped>
.dialog-content {
  padding: 0;
}
.dialog-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.danger-text {
  color: #f56c6c;
  font-weight: bold;
}
</style>
