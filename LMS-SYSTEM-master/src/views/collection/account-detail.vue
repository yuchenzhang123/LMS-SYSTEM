<template>
  <div class="detail-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>催收详情</span>
        <el-button type="text" icon="el-icon-back" @click="goBack">{{ backButtonText }}</el-button>
      </div>

      <loan-info-section :detail="detail" />

      <el-divider></el-divider>

      <el-tabs value="collect">
        <el-tab-pane label="催收记录" name="collect">
          <collection-record-tab
            :records="displayCollectionRecords"
            :sms-loading="smsLoading"
            :read-only="isReadOnly"
            @sendSms="sendSmsCollect"
            @openRecordDialog="openRecordDialog"
            @exportMaterial="exportMaterial"
            @openMaterialUpdateDialog="openMaterialUpdateDialog"
          />
        </el-tab-pane>

        <el-tab-pane label="诉讼信息" name="litigation">
          <litigation-tab
            :litigation-list="litigationList"
            :read-only="isReadOnly"
            @openNewLitigation="openNewLitigation"
            @openLitigationDetail="openLitigationDetail"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 催收登记对话框 -->
    <collection-record-dialog
      :visible.sync="recordDialogVisible"
      :customer-name="detail.customerName"
      :collection-method-options="collectionMethodOptions"
      @submit="submitManualRecord"
    />

    <!-- 诉讼详情对话框 -->
    <litigation-dialog
      :visible.sync="litigationDrawerVisible"
      :is-edit-mode="isEditMode"
      :submit-loading="litigationSubmitLoading"
      :current-litigation="currentLitigation"
      :litigation-form="litigationForm"
      :litigation-status-options="litigationStatusOptions"
      @toggleEditMode="toggleEditMode"
      @cancelEdit="cancelEdit"
      @submitProgress="submitLitigationProgress"
      @close="handleLitigationDialogClose"
    />

    <!-- 材料补交/重交对话框 -->
    <material-update-dialog
      :visible.sync="materialUpdateDialogVisible"
      :record-id="materialUpdateForm.recordId"
      :loading="materialUpdateLoading"
      ref="materialUpdateDialog"
      @submit="submitMaterialUpdate"
    />
  </div>
</template>

<script>
import { Message } from 'element-ui'
import { downloadBlob } from '@/utils/file-download'
import { uploadFileApi, downloadMaterialApi } from '@/api/collection'
import LoanInfoSection from '@/components/collection/LoanInfoSection.vue'
import CollectionRecordTab from '@/components/collection/CollectionRecordTab.vue'
import LitigationTab from '@/components/collection/LitigationTab.vue'
import CollectionRecordDialog from '@/components/collection/CollectionRecordDialog.vue'
import MaterialUpdateDialog from '@/components/collection/MaterialUpdateDialog.vue'
import LitigationDialog from '@/components/collection/LitigationDialog.vue'

export default {
  name: 'AccountDetail',
  components: {
    LoanInfoSection,
    CollectionRecordTab,
    LitigationTab,
    CollectionRecordDialog,
    MaterialUpdateDialog,
    LitigationDialog
  },
  data () {
    return {
      recordDialogVisible: false,
      litigationDrawerVisible: false,
      isEditMode: false,
      litigationSubmitLoading: false,
      smsLoading: false,
      currentLitigation: {},
      materialUpdateForm: {
        recordId: '',
        materialType: '',
        materialName: '',
        materialUrl: ''
      },
      materialUpdateDialogVisible: false,
      materialUpdateLoading: false,
      collectionMethodOptions: [
        { value: 'phone', label: '电话', materialLabel: '电话录音', materialType: 'audio' },
        { value: 'visit', label: '上门催收', materialLabel: '上门记录', materialType: 'image' },
        { value: 'mail', label: '邮寄催收函', materialLabel: '催收函', materialType: 'image' },
        { value: 'sms', label: '短信', materialLabel: '短信记录', materialType: 'document' },
        { value: 'other', label: '其他', materialLabel: '其他材料', materialType: 'document' }
      ],
      litigationForm: {
        litigationId: '',
        statusCode: '',
        submitToLawFirmDate: '',
        submitToCourtDate: '',
        filingCaseNo: '',
        isHearing: false,
        hearingDate: '',
        judgmentDate: '',
        executionApplyToCourtDate: '',
        executionFilingDate: '',
        executionCaseNo: '',
        auctionStatus: '',
        litigationFee: '',
        litigationFeePaidByCustomer: false,
        preservationFee: '',
        preservationFeePaidByCustomer: false,
        appraisalFee: '',
        litigationPreservationPaidAt: '',
        litigationPreservationWriteOffAt: '',
        lawyerFee: '',
        lawyerFeePaidByCustomer: false,
        courtName: '',
        lawFirm: '',
        remark: ''
      },
      litigationStatusOptions: [
        { code: '1.1', label: '未起诉', inLitigation: false },
        { code: '1.2', label: '未起诉（已正常还款）', inLitigation: false },
        { code: '1.3', label: '准备材料起诉', inLitigation: true },
        { code: '1.4', label: '提交律所', inLitigation: true },
        { code: '1.5', label: '撤诉', inLitigation: false },
        { code: '1.5.1', label: '撤诉（借款人死亡）', inLitigation: false },
        { code: '1.6', label: '法院不予受理', inLitigation: false },
        { code: '1.7', label: '已结清', inLitigation: false },
        { code: '2.1', label: '已起诉待立案', inLitigation: true },
        { code: '2.2', label: '已立案待开庭', inLitigation: true },
        { code: '2.3', label: '已开庭待判决', inLitigation: true },
        { code: '3.1', label: '已判决待申请执行', inLitigation: true },
        { code: '3.2', label: '已申请执行待执行立案', inLitigation: true },
        { code: '3.3', label: '已执行立案', inLitigation: true },
        { code: '3.3.1', label: '执行拍卖中', inLitigation: true },
        { code: '3.3.2', label: '申请恢复执行', inLitigation: true },
        { code: '3.4', label: '已拍卖成功，待法院扣划', inLitigation: true },
        { code: '3.5', label: '中止执行', inLitigation: true },
        { code: '3.6', label: '终结本次执行', inLitigation: true },
        { code: '3.7', label: '终结执行【注意2年内恢复执行，一般3个月内恢复执行】', inLitigation: false },
        { code: '3.8', label: '申请再次恢复执行', inLitigation: true },
        { code: '3.9', label: '调解结案', inLitigation: false },
        { code: '3.9.2', label: '调解后仍未正常还款，拟恢复执行', inLitigation: true }
      ]
    }
  },
  created () {
    this.loadRemoteDetailData()
  },
  computed: {
    isReadOnly () {
      return this.$route.query.readOnly === 'true' || this.$route.query.readOnly === true
    },
    detail () {
      const selectedAccount = this.$store.state.collection && this.$store.state.collection.selectedAccount
      return {
        loanAccount: selectedAccount && selectedAccount.loanAccount ? selectedAccount.loanAccount : (this.$route.query.loanAccount || '--'),
        customerId: selectedAccount && selectedAccount.customerId ? selectedAccount.customerId : '--',
        customerName: selectedAccount && selectedAccount.customerName ? selectedAccount.customerName : '--',
        orgName: selectedAccount && selectedAccount.orgName ? selectedAccount.orgName : '--',
        phone: selectedAccount && selectedAccount.phone ? selectedAccount.phone : '--',
        productCode: selectedAccount && selectedAccount.productCode ? selectedAccount.productCode : '--',
        loanDate: selectedAccount && selectedAccount.loanDate ? selectedAccount.loanDate : '--',
        loanTerm: selectedAccount && selectedAccount.loanTerm ? selectedAccount.loanTerm : 0,
        overdueDays: Number(selectedAccount && selectedAccount.overdueDays ? selectedAccount.overdueDays : 0),
        contractAmount: selectedAccount && selectedAccount.contractAmount ? selectedAccount.contractAmount : '--',
        loanBalance: selectedAccount && selectedAccount.loanBalance ? selectedAccount.loanBalance : '--',
        unexpiredPrincipal: selectedAccount && selectedAccount.unexpiredPrincipal ? selectedAccount.unexpiredPrincipal : '--',
        overduePrincipal: selectedAccount && selectedAccount.overduePrincipal ? selectedAccount.overduePrincipal : '--',
        overdueInterest: selectedAccount && selectedAccount.overdueInterest ? selectedAccount.overdueInterest : '--',
        overduePenalty: selectedAccount && selectedAccount.overduePenalty ? selectedAccount.overduePenalty : '--',
        totalOverdueAmount: selectedAccount && selectedAccount.totalOverdueAmount ? selectedAccount.totalOverdueAmount : '--'
      }
    },
    accountEntrySource () {
      return this.$store.state.collection && this.$store.state.collection.selectedAccountSource
        ? this.$store.state.collection.selectedAccountSource
        : 'list'
    },
    backButtonText () {
      return this.accountEntrySource === 'notice' ? '返回通知详情' : '返回清单'
    },
    displayCollectionRecords () {
      const loanAccount = this.detail.loanAccount
      return this.$store.getters['collection/getCollectionRecordsByLoanAccount'](loanAccount)
    },
    litigationList () {
      return this.$store.getters['collection/getLitigationListByLoanAccount'](this.detail.loanAccount)
    },
    smsTemplateContent () {
      return `尊敬的${this.detail.customerName}，您的贷款账户${this.detail.loanAccount}已逾期${this.detail.overdueDays}天，请尽快处理还款。`
    }
  },
  methods: {
    async loadRemoteDetailData () {
      const loanAccount = this.detail.loanAccount
      if (!loanAccount || loanAccount === '--') {
        return
      }
      try {
        await this.$store.dispatch('collection/loadAccountDetailData', loanAccount)
      } catch (e) {
        // 错误已在 request.js 中统一处理显示
        console.warn('账户详情加载失败:', e.message)
      }
    },
    goBack () {
      if (this.accountEntrySource === 'notice') {
        this.$router.push('/notice/detail')
        return
      }
      this.$router.push('/collection/account-list')
    },
    async sendSmsCollect () {
      if (!this.detail.loanAccount || this.detail.loanAccount === '--') {
        Message.warning('未识别到贷款账户信息，无法发送短信')
        return
      }
      this.smsLoading = true
      try {
        const { remoteSuccess } = await this.$store.dispatch('collection/sendSmsCollection', {
          loanAccount: this.detail.loanAccount,
          customerId: this.detail.customerId,
          content: this.smsTemplateContent,
          phone: this.detail.phone
        })
        await this.loadRemoteDetailData()
        if (remoteSuccess) {
          Message.success('短信催收已执行并生成催收记录')
        } else {
          Message.warning('短信接口暂不可用，已登记本次催收记录')
        }
      } catch (e) {
        // 错误已在 request.js 中统一处理显示
        console.warn('短信催收执行失败:', e.message)
      } finally {
        this.smsLoading = false
      }
    },
    async submitManualRecord ({ form, rawFile }) {
      if (!form.result) {
        Message.warning('请填写催收结果')
        return
      }
      try {
        let materialUrl = ''
        let materialName = ''
        const methodOption = this.collectionMethodOptions.find(item => item.value === form.method)
        const methodText = methodOption ? methodOption.label : form.method
        const materialType = rawFile && methodOption ? methodOption.materialType : ''

        // 如果有文件，先上传
        if (rawFile) {
          const formData = new FormData()
          formData.append('file', rawFile)
          formData.append('materialType', materialType)
          formData.append('materialName', rawFile.name)

          const uploadRes = await uploadFileApi(formData)
          materialUrl = uploadRes.data.url
          materialName = rawFile.name
        }

        // 构建FormData发送到后端（使用multipart/form-data格式）
        const formData = new FormData()
        formData.append('loanAccount', this.detail.loanAccount)
        formData.append('customerId', this.detail.customerId)
        formData.append('targetType', form.targetType || '')
        formData.append('targetName', form.targetName || '')
        formData.append('actualCollectionTime', form.actualCollectionTime || '')
        formData.append('method', form.method)
        formData.append('methodText', methodText)
        formData.append('result', form.result)
        formData.append('remark', form.remark || '')
        formData.append('materialType', materialType || '')
        formData.append('materialName', materialName || '')
        if (rawFile) {
          formData.append('file', rawFile)
        }

        // 直接调用API而不是通过store
        await this.$store.dispatch('collection/addManualCollectionRecord', {
          loanAccount: this.detail.loanAccount,
          customerId: this.detail.customerId,
          targetType: form.targetType,
          targetName: form.targetName,
          actualCollectionTime: form.actualCollectionTime,
          method: form.method,
          methodText: methodText,
          result: form.result,
          remark: form.remark,
          materialType: materialType,
          materialName: materialName,
          materialUrl: materialUrl
        })

        this.recordDialogVisible = false
        await this.loadRemoteDetailData()
        Message.success('催收记录登记成功')
      } catch (e) {
        console.error('提交催收记录失败:', e)
        // 错误已在 request.js 中统一处理显示
      }
    },
    openRecordDialog () {
      this.recordDialogVisible = true
    },
    async exportMaterial (record) {
      try {
        const blob = await downloadMaterialApi(record.id)
        downloadBlob(blob, record.materialName || 'material-file')
      } catch (e) {
        console.error('下载材料失败:', e)
        // 错误已在 request.js 中统一处理显示
      }
    },
    openMaterialUpdateDialog (row) {
      this.materialUpdateDialogVisible = true
      this.$nextTick(() => {
        this.$refs.materialUpdateDialog.setInitialData(row)
      })
    },
    async submitMaterialUpdate ({ form, rawFile }) {
      if (!rawFile) {
        Message.warning('请选择要上传的材料文件')
        return
      }
      this.materialUpdateLoading = true
      try {
        await this.$store.dispatch('collection/updateCollectionMaterial', {
          recordId: form.recordId,
          loanAccount: this.detail.loanAccount,
          materialType: form.materialType,
          materialName: rawFile.name,
          materialUrl: form.materialUrl,
          rawFile: rawFile // 传递文件对象
        })

        this.materialUpdateDialogVisible = false
        await this.loadRemoteDetailData()
        Message.success('材料更新成功')
      } catch (e) {
        console.error('材料更新失败:', e)
        // 错误已在 request.js 中统一处理显示
      } finally {
        this.materialUpdateLoading = false
      }
    },
    openLitigationDetail (row) {
      this.currentLitigation = { ...row }
      this.isEditMode = false
      this.litigationDrawerVisible = true
      this.initLitigationForm(row)
    },
    openNewLitigation () {
      this.currentLitigation = {}
      this.isEditMode = true
      this.litigationDrawerVisible = true
      this.resetLitigationForm()
    },
    initLitigationForm (row) {
      this.litigationForm = {
        litigationId: row.litigationId || '',
        statusCode: row.statusCode || '',
        submitToLawFirmDate: row.submitToLawFirmDate || '',
        submitToCourtDate: row.submitToCourtDate || '',
        filingCaseNo: row.filingCaseNo || '',
        isHearing: !!row.isHearing,
        hearingDate: row.hearingDate || '',
        judgmentDate: row.judgmentDate || '',
        executionApplyToCourtDate: row.executionApplyToCourtDate || '',
        executionFilingDate: row.executionFilingDate || '',
        executionCaseNo: row.executionCaseNo || '',
        auctionStatus: row.auctionStatus || '',
        litigationFee: row.litigationFee || '',
        litigationFeePaidByCustomer: !!row.litigationFeePaidByCustomer,
        preservationFee: row.preservationFee || '',
        preservationFeePaidByCustomer: !!row.preservationFeePaidByCustomer,
        appraisalFee: row.appraisalFee || '',
        litigationPreservationPaidAt: row.litigationPreservationPaidAt || '',
        litigationPreservationWriteOffAt: row.litigationPreservationWriteOffAt || '',
        lawyerFee: row.lawyerFee || '',
        lawyerFeePaidByCustomer: !!row.lawyerFeePaidByCustomer,
        courtName: row.courtName || '',
        lawFirm: row.lawFirm || '',
        remark: row.remark || ''
      }
    },
    resetLitigationForm () {
      this.litigationForm = {
        litigationId: '',
        statusCode: '',
        submitToLawFirmDate: '',
        submitToCourtDate: '',
        filingCaseNo: '',
        isHearing: false,
        hearingDate: '',
        judgmentDate: '',
        executionApplyToCourtDate: '',
        executionFilingDate: '',
        executionCaseNo: '',
        auctionStatus: '',
        litigationFee: '',
        litigationFeePaidByCustomer: false,
        preservationFee: '',
        preservationFeePaidByCustomer: false,
        appraisalFee: '',
        litigationPreservationPaidAt: '',
        litigationPreservationWriteOffAt: '',
        lawyerFee: '',
        lawyerFeePaidByCustomer: false,
        courtName: '',
        lawFirm: '',
        remark: ''
      }
    },
    toggleEditMode () {
      this.isEditMode = true
    },
    cancelEdit () {
      this.isEditMode = false
    },
    handleLitigationDialogClose () {
      this.isEditMode = false
    },
    async submitLitigationProgress () {
      if (!this.detail.loanAccount || this.detail.loanAccount === '--') {
        Message.warning('未识别到贷款账户信息，无法登记诉讼进度')
        return
      }
      if (!this.litigationForm.statusCode) {
        Message.warning('请选择诉讼状态')
        return
      }
      const statusMeta = this.litigationStatusOptions.find(item => item.code === this.litigationForm.statusCode)
      if (!statusMeta) {
        Message.warning('诉讼状态无效，请重新选择')
        return
      }
      this.litigationSubmitLoading = true
      try {
        const response = await this.$store.dispatch('collection/updateLitigationProgress', {
          litigationId: this.litigationForm.litigationId || '',
          loanAccount: this.detail.loanAccount,
          customerId: this.detail.customerId,
          statusCode: statusMeta.code,
          statusText: statusMeta.label,
          inLitigation: statusMeta.inLitigation,
          submitToLawFirmDate: this.litigationForm.submitToLawFirmDate,
          submitToCourtDate: this.litigationForm.submitToCourtDate,
          filingCaseNo: this.litigationForm.filingCaseNo,
          isHearing: this.litigationForm.isHearing,
          hearingDate: this.litigationForm.hearingDate,
          judgmentDate: this.litigationForm.judgmentDate,
          executionApplyToCourtDate: this.litigationForm.executionApplyToCourtDate,
          executionFilingDate: this.litigationForm.executionFilingDate,
          executionCaseNo: this.litigationForm.executionCaseNo,
          auctionStatus: this.litigationForm.auctionStatus,
          litigationFee: this.litigationForm.litigationFee,
          litigationFeePaidByCustomer: this.litigationForm.litigationFeePaidByCustomer,
          preservationFee: this.litigationForm.preservationFee,
          preservationFeePaidByCustomer: this.litigationForm.preservationFeePaidByCustomer,
          appraisalFee: this.litigationForm.appraisalFee,
          litigationPreservationPaidAt: this.litigationForm.litigationPreservationPaidAt,
          litigationPreservationWriteOffAt: this.litigationForm.litigationPreservationWriteOffAt,
          lawyerFee: this.litigationForm.lawyerFee,
          lawyerFeePaidByCustomer: this.litigationForm.lawyerFeePaidByCustomer,
          courtName: this.litigationForm.courtName,
          lawFirm: this.litigationForm.lawFirm,
          remark: this.litigationForm.remark
        })
        if (response && response.litigationInfo) {
          this.currentLitigation = { ...response.litigationInfo }
          this.litigationForm.litigationId = response.litigationInfo.litigationId
        }
        this.isEditMode = false
        await this.loadRemoteDetailData()
        Message.success('诉讼进度登记成功，并已写入催收记录')
      } catch (e) {
        console.error('诉讼进度登记失败:', e)
        // 错误已在 request.js 中统一处理显示
      } finally {
        this.litigationSubmitLoading = false
      }
    }
  }
}
</script>

<style scoped>
.detail-container {
  padding: 10px;
}
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
