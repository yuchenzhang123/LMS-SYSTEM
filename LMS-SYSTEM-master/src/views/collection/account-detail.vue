<template>
  <div class="detail-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>催收详情</span>
        <el-button type="text" icon="el-icon-back" @click="goBack">{{ backButtonText }}</el-button>
      </div>

      <el-descriptions title="贷款信息" :column="3" border>
        <el-descriptions-item label="机构名">{{ detail.orgName }}</el-descriptions-item>
        <el-descriptions-item label="客户号">{{ detail.customerId }}</el-descriptions-item>
        <el-descriptions-item label="客户名">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="产品码">{{ detail.productCode }}</el-descriptions-item>
        <el-descriptions-item label="贷款账号">{{ detail.loanAccount }}</el-descriptions-item>
        <el-descriptions-item label="放款日期">{{ detail.loanDate }}</el-descriptions-item>
        <el-descriptions-item label="期限(月)">{{ detail.loanTerm }}</el-descriptions-item>
        <el-descriptions-item label="逾期天数">
          <el-tag :type="detail.overdueDays > 30 ? 'danger' : 'warning'">
            {{ detail.overdueDays }} 天
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-descriptions title="金额信息" :column="3" border style="margin-top: 12px;">
        <el-descriptions-item label="贷款合同金额">{{ detail.contractAmount }} 元</el-descriptions-item>
        <el-descriptions-item label="贷款余额">{{ detail.loanBalance }} 元</el-descriptions-item>
        <el-descriptions-item label="未到期本金">{{ detail.unexpiredPrincipal }} 元</el-descriptions-item>
        <el-descriptions-item label="拖欠本金余额">
          <span class="danger-text">{{ detail.overduePrincipal }} 元</span>
        </el-descriptions-item>
        <el-descriptions-item label="拖欠利息">{{ detail.overdueInterest }} 元</el-descriptions-item>
        <el-descriptions-item label="拖欠罚息">{{ detail.overduePenalty }} 元</el-descriptions-item>
        <el-descriptions-item label="逾期利息（含罚息）" :span="3">
          <span class="danger-text">{{ detail.totalOverdueAmount }} 元</span>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider></el-divider>

      <el-tabs value="collect">
        <el-tab-pane label="催收记录" name="collect">
          <div class="toolbar-row">
            <div class="right-tools">
              <el-button size="small" type="primary" @click="sendSmsCollect" :loading="smsLoading">短信催收</el-button>
              <el-button size="small" @click="openRecordDialog">登记催收记录</el-button>
            </div>
          </div>

          <el-table :data="displayCollectionRecords" border stripe style="width: 100%">
            <el-table-column prop="time" label="催收时间" width="170"></el-table-column>
            <el-table-column prop="methodText" label="方式" width="90"></el-table-column>
            <el-table-column prop="result" label="催收结果" min-width="220"></el-table-column>
            <el-table-column prop="operatorName" label="业务员" width="110"></el-table-column>
            <el-table-column prop="remark" label="备注" min-width="180"></el-table-column>
            <el-table-column prop="materialName" label="附件材料" min-width="180"></el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="mini" :disabled="!scope.row.materialUrl" @click="exportMaterial(scope.row)">
                  导出
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="诉讼信息" name="litigation">
          <div class="toolbar-row">
            <div class="right-tools">
              <el-button size="small" type="primary" @click="openNewLitigation">新增诉讼</el-button>
            </div>
          </div>
          <el-table :data="litigationList" border stripe style="width: 100%">
            <el-table-column prop="statusText" label="诉讼状态" min-width="200">
              <template slot-scope="scope">
                <el-tag :type="scope.row.inLitigation ? 'warning' : 'success'" size="small">
                  {{ scope.row.statusText || '未登记' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="诉讼备注" min-width="200"></el-table-column>
            <el-table-column prop="updatedAt" label="最近更新时间" width="170"></el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click="openLitigationDetail(scope.row)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog title="登记催收记录" :visible.sync="recordDialogVisible" width="620px">
      <el-form :model="recordForm" label-width="110px">
        <el-form-item label="催收方式">
          <el-select v-model="recordForm.method" style="width: 100%;">
            <el-option label="电话" value="phone"></el-option>
            <el-option label="上门" value="visit"></el-option>
            <el-option label="邮件" value="email"></el-option>
            <el-option label="其他" value="other"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="催收结果">
          <el-input v-model="recordForm.result" placeholder="请输入本次催收结果"></el-input>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="recordForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          ></el-input>
        </el-form-item>
        <el-form-item label="电话录音" v-if="recordForm.method === 'phone'">
          <el-upload
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            :on-change="onRecordingFileChange"
            :file-list="uploadFileList"
            :limit="1"
          >
            <el-button size="small" type="primary">选择录音文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="recordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitManualRecord">确认登记</el-button>
      </span>
    </el-dialog>

    <!-- 诉讼详情抽屉 -->
    <el-drawer
      :title="litigationDrawerTitle"
      :visible.sync="litigationDrawerVisible"
      direction="rtl"
      size="50%"
    >
      <div class="litigation-drawer-content">
        <div class="drawer-toolbar">
          <el-button type="primary" size="small" @click="toggleEditMode" v-if="!isEditMode">
            登记诉讼进度
          </el-button>
          <el-button size="small" @click="cancelEdit" v-if="isEditMode">取消</el-button>
          <el-button type="primary" size="small" @click="submitLitigationProgress" v-if="isEditMode" :loading="litigationSubmitLoading">保存</el-button>
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
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('submitToLawFirmDate')" label="提交律所时间">{{ currentLitigation.submitToLawFirmDate || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('lawFirm')" label="律所名称">{{ currentLitigation.lawFirm || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('submitToCourtDate')" label="提交法院时间">{{ currentLitigation.submitToCourtDate || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('courtName')" label="涉及法院">{{ currentLitigation.courtName || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('filingCaseNo')" label="诉讼立案案号">{{ currentLitigation.filingCaseNo || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('isHearing')" label="是否开庭">{{ currentLitigation.isHearing ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('hearingDate')" label="开庭时间">{{ currentLitigation.hearingDate || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('judgmentDate')" label="判决时间">{{ currentLitigation.judgmentDate || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('executionApplyToCourtDate')" label="执行申请书提交法院时间">{{ currentLitigation.executionApplyToCourtDate || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('executionFilingDate')" label="执行立案时间">{{ currentLitigation.executionFilingDate || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('executionCaseNo')" label="执行立案案号">{{ currentLitigation.executionCaseNo || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('auctionStatus')" label="拍卖状态">{{ currentLitigation.auctionStatus || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('litigationFee')" label="诉讼费">
            <span :class="{ 'danger-text': currentLitigation.litigationFeePaidByCustomer }">{{ currentLitigation.litigationFee || '--' }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('preservationFee')" label="保全费">
            <span :class="{ 'danger-text': currentLitigation.preservationFeePaidByCustomer }">{{ currentLitigation.preservationFee || '--' }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('appraisalFee')" label="评估费">{{ currentLitigation.appraisalFee || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('litigationPreservationPaidAt')" label="诉讼和保全支付时间">{{ currentLitigation.litigationPreservationPaidAt || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('litigationPreservationWriteOffAt')" label="诉讼和保全销账时间">{{ currentLitigation.litigationPreservationWriteOffAt || '--' }}</el-descriptions-item>
          <el-descriptions-item v-if="displayLitigationFieldKeys.includes('lawyerFee')" label="律师费">
            <span :class="{ 'danger-text': currentLitigation.lawyerFeePaidByCustomer }">{{ currentLitigation.lawyerFee || '--' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="诉讼备注" :span="2">{{ currentLitigation.remark || '--' }}</el-descriptions-item>
        </el-descriptions>

        <el-form v-if="isEditMode" :model="litigationForm" label-width="140px" style="padding: 20px;">
          <el-form-item label="诉讼状态">
            <el-select v-model="litigationForm.statusCode" filterable style="width: 100%;">
              <el-option
                v-for="item in litigationStatusOptions"
                :key="item.code"
                :label="`${item.code} ${item.label}`"
                :value="item.code"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="提交律所时间" v-if="isLitigationFieldVisible('submitToLawFirmDate')">
            <el-date-picker
              v-model="litigationForm.submitToLawFirmDate"
              type="date"
              value-format="yyyy/M/d"
              placeholder="请选择提交律所时间"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="律所名称" v-if="isLitigationFieldVisible('lawFirm')">
            <el-input v-model="litigationForm.lawFirm" placeholder="请输入律所名称"></el-input>
          </el-form-item>
          <el-form-item label="提交法院时间" v-if="isLitigationFieldVisible('submitToCourtDate')">
            <el-date-picker
              v-model="litigationForm.submitToCourtDate"
              type="date"
              value-format="yyyy/M/d"
              placeholder="请选择提交法院时间"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="涉及法院" v-if="isLitigationFieldVisible('courtName')">
            <el-input v-model="litigationForm.courtName" placeholder="请输入涉及法院"></el-input>
          </el-form-item>
          <el-form-item label="诉讼立案案号" v-if="isLitigationFieldVisible('filingCaseNo')">
            <el-input v-model="litigationForm.filingCaseNo" placeholder="请输入诉讼立案案号"></el-input>
          </el-form-item>
          <el-form-item label="是否开庭" v-if="isLitigationFieldVisible('isHearing')">
            <el-switch
              v-model="litigationForm.isHearing"
              :active-value="true"
              :inactive-value="false"
              active-text="是"
              inactive-text="否"
            ></el-switch>
          </el-form-item>
          <el-form-item label="开庭时间" v-if="isLitigationFieldVisible('hearingDate')">
            <el-date-picker
              v-model="litigationForm.hearingDate"
              type="date"
              value-format="yyyy/M/d"
              placeholder="请选择开庭时间"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="判决时间" v-if="isLitigationFieldVisible('judgmentDate')">
            <el-date-picker
              v-model="litigationForm.judgmentDate"
              type="date"
              value-format="yyyy/M/d"
              placeholder="请选择判决时间"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="执行申请书提交法院时间" v-if="isLitigationFieldVisible('executionApplyToCourtDate')">
            <el-date-picker
              v-model="litigationForm.executionApplyToCourtDate"
              type="date"
              value-format="yyyy/M/d"
              placeholder="请选择执行申请书提交时间"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="执行立案时间" v-if="isLitigationFieldVisible('executionFilingDate')">
            <el-date-picker
              v-model="litigationForm.executionFilingDate"
              type="date"
              value-format="yyyy/M/d"
              placeholder="请选择执行立案时间"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="执行立案案号" v-if="isLitigationFieldVisible('executionCaseNo')">
            <el-input v-model="litigationForm.executionCaseNo" placeholder="请输入执行立案案号"></el-input>
          </el-form-item>
          <el-form-item label="拍卖状态" v-if="isLitigationFieldVisible('auctionStatus')">
            <el-input v-model="litigationForm.auctionStatus" placeholder="例如：一拍、二拍、变卖流拍"></el-input>
          </el-form-item>
          <el-form-item label="诉讼费" v-if="isLitigationFieldVisible('litigationFee')">
            <el-input v-model="litigationForm.litigationFee" placeholder="请输入诉讼费金额">
              <template slot="append">元</template>
            </el-input>
            <el-checkbox v-model="litigationForm.litigationFeePaidByCustomer" style="margin-top: 8px;">客户已支付</el-checkbox>
          </el-form-item>
          <el-form-item label="保全费" v-if="isLitigationFieldVisible('preservationFee')">
            <el-input v-model="litigationForm.preservationFee" placeholder="请输入保全费金额">
              <template slot="append">元</template>
            </el-input>
            <el-checkbox v-model="litigationForm.preservationFeePaidByCustomer" style="margin-top: 8px;">客户已支付</el-checkbox>
          </el-form-item>
          <el-form-item label="评估费" v-if="isLitigationFieldVisible('appraisalFee')">
            <el-input v-model="litigationForm.appraisalFee" placeholder="请输入评估费金额">
              <template slot="append">元</template>
            </el-input>
          </el-form-item>
          <el-form-item label="诉讼和保全支付时间" v-if="isLitigationFieldVisible('litigationPreservationPaidAt')">
            <el-date-picker
              v-model="litigationForm.litigationPreservationPaidAt"
              type="date"
              value-format="yyyy/M/d"
              placeholder="请选择诉讼和保全支付时间"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="诉讼和保全销账时间" v-if="isLitigationFieldVisible('litigationPreservationWriteOffAt')">
            <el-date-picker
              v-model="litigationForm.litigationPreservationWriteOffAt"
              type="date"
              value-format="yyyy/M/d"
              placeholder="请选择诉讼和保全销账时间"
              style="width: 100%;"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="律师费" v-if="isLitigationFieldVisible('lawyerFee')">
            <el-input v-model="litigationForm.lawyerFee" placeholder="请输入律师费金额">
              <template slot="append">元</template>
            </el-input>
            <el-checkbox v-model="litigationForm.lawyerFeePaidByCustomer" style="margin-top: 8px;">客户已支付</el-checkbox>
          </el-form-item>
          <el-form-item label="进度备注">
            <el-input
              v-model="litigationForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入诉讼进度备注"
            ></el-input>
          </el-form-item>
        </el-form>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { Message } from 'element-ui'
import { safeDownloadFileByUrl } from '@/utils/file-download'

export default {
  name: 'AccountDetail',
  data () {
    return {
      recordDialogVisible: false,
      litigationDrawerVisible: false,
      isEditMode: false,
      litigationSubmitLoading: false,
      smsLoading: false,
      recordForm: {
        method: 'phone',
        result: '',
        remark: ''
      },
      currentLitigation: {},
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
      ],
      uploadFileList: [],
      recordingRawFile: null
    }
  },
  created () {
    this.loadRemoteDetailData()
  },
  computed: {
    userInfo () {
      return this.$store.state.permission && this.$store.state.permission.userInfo ? this.$store.state.permission.userInfo : null
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
    litigationDrawerTitle () {
      return this.isEditMode ? '登记诉讼进度' : '诉讼详情'
    },
    currentLitigationStatusCode () {
      if (this.isEditMode && this.litigationForm.statusCode) {
        return this.litigationForm.statusCode
      }
      return this.currentLitigation && this.currentLitigation.statusCode ? this.currentLitigation.statusCode : ''
    },
    displayLitigationFieldKeys () {
      return this.getLitigationFieldKeysByStatus(this.currentLitigationStatusCode)
    },
    smsTemplateContent () {
      return `尊敬的${this.detail.customerName}，您的贷款账户${this.detail.loanAccount}已逾期${this.detail.overdueDays}天，请尽快处理还款。`
    }
  },
  methods: {
    getLitigationFieldKeysByStatus (statusCode) {
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
    isLitigationFieldVisible (fieldKey) {
      return this.getLitigationFieldKeysByStatus(this.litigationForm.statusCode).includes(fieldKey)
    },
    async loadRemoteDetailData () {
      const loanAccount = this.detail.loanAccount
      if (!loanAccount || loanAccount === '--') {
        return
      }
      try {
        await this.$store.dispatch('collection/loadAccountDetailData', loanAccount)
      } catch (e) {
        Message.warning('账户详情加载失败，已显示本地缓存数据')
      }
    },
    goBack () {
      if (this.accountEntrySource === 'notice') {
        this.$router.push('/notice/detail')
        return
      }
      this.$router.push('/collection/account-list')
    },
    openRecordDialog () {
      this.recordDialogVisible = true
      this.recordForm = {
        method: 'phone',
        result: '',
        remark: ''
      }
      this.uploadFileList = []
      this.recordingRawFile = null
    },
    openLitigationDetail (row) {
      this.currentLitigation = { ...row }
      this.isEditMode = false
      this.litigationDrawerVisible = true
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
    openNewLitigation () {
      // 打开新增诉讼信息界面
      this.currentLitigation = {}
      this.isEditMode = true
      this.litigationDrawerVisible = true
      this.litigationForm = {
        litigationId: '', // 空值表示新增
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
        // 重新加载账户详情数据，更新催收记录列表
        await this.loadRemoteDetailData()
        if (remoteSuccess) {
          Message.success('短信催收已执行并生成催收记录')
        } else {
          Message.warning('短信接口暂不可用，已登记本次催收记录')
        }
      } catch (e) {
        Message.warning('短信催收执行异常，请稍后重试')
      } finally {
        this.smsLoading = false
      }
    },
    onRecordingFileChange (file, fileList) {
      this.uploadFileList = fileList.slice(-1)
      this.recordingRawFile = file.raw || null
    },
    async submitManualRecord () {
      if (!this.recordForm.result) {
        Message.warning('请填写催收结果')
        return
      }
      if (this.recordForm.method === 'phone' && !this.recordingRawFile) {
        Message.warning('电话催收请上传录音文件')
        return
      }
      let recordingUrl = ''
      let recordingName = ''
      if (this.recordingRawFile) {
        recordingUrl = URL.createObjectURL(this.recordingRawFile)
        recordingName = this.recordingRawFile.name
      }
      const methodTextMap = {
        phone: '电话',
        visit: '上门',
        email: '邮件',
        other: '其他'
      }
      await this.$store.dispatch('collection/addManualCollectionRecord', {
        loanAccount: this.detail.loanAccount,
        customerId: this.detail.customerId,
        method: this.recordForm.method,
        methodText: methodTextMap[this.recordForm.method],
        result: this.recordForm.result,
        remark: this.recordForm.remark,
        materialType: this.recordForm.method === 'phone' ? 'audio' : '',
        materialName: recordingName,
        materialUrl: recordingUrl
      })
      this.recordDialogVisible = false
      // 重新加载账户详情数据，更新催收记录列表
      await this.loadRemoteDetailData()
      Message.success('催收记录登记成功')
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
        // 更新当前诉讼信息
        if (response && response.litigationInfo) {
          this.currentLitigation = { ...response.litigationInfo }
          this.litigationForm.litigationId = response.litigationInfo.litigationId
        }
        this.isEditMode = false
        // 重新加载账户详情数据，更新催收记录和诉讼信息列表
        await this.loadRemoteDetailData()
        Message.success('诉讼进度登记成功，并已写入催收记录')
      } catch (e) {
        Message.error('诉讼进度登记失败')
      } finally {
        this.litigationSubmitLoading = false
      }
    },
    async exportMaterial (record) {
      const { success, message } = await safeDownloadFileByUrl({
        url: record.materialUrl,
        fileName: record.materialName || 'material-file'
      })
      if (!success) {
        Message.warning(message)
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
.danger-text {
  color: #f56c6c;
  font-weight: bold;
}
.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.right-tools {
  display: flex;
  gap: 8px;
}
.litigation-drawer-content {
  padding: 0 20px 20px;
}
.drawer-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}
</style>
