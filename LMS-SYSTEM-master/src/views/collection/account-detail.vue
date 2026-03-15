<template>
  <div class="detail-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>账户详情</span>
        <el-button type="text" icon="el-icon-back" @click="goBack">{{ backButtonText }}</el-button>
      </div>

      <el-descriptions title="基础信息" :column="3" border>
        <el-descriptions-item label="客户号">{{ detail.customerId }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ detail.productName }}</el-descriptions-item>
        <el-descriptions-item label="逾期本金">
          <span class="danger-text">{{ detail.overdueAmount }} 元</span>
        </el-descriptions-item>
        <el-descriptions-item label="逾期天数">
          <el-tag :type="detail.overdueDays > 30 ? 'danger' : 'warning'">
            {{ detail.overdueDays }} 天
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最后催收时间">{{ detail.lastCallDate }}</el-descriptions-item>
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

        <el-tab-pane label="信贷记录" name="credit">
          <el-table :data="creditRecords" border stripe style="width: 100%">
            <el-table-column prop="occurTime" label="时间" width="170"></el-table-column>
            <el-table-column prop="type" label="类型" width="100"></el-table-column>
            <el-table-column prop="amount" label="金额(元)" width="140"></el-table-column>
            <el-table-column prop="remark" label="说明"></el-table-column>
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
      smsLoading: false,
      recordForm: {
        method: 'phone',
        result: '',
        remark: ''
      },
      uploadFileList: [],
      recordingRawFile: null
    }
  },
  computed: {
    userInfo () {
      return this.$store.state.permission && this.$store.state.permission.userInfo ? this.$store.state.permission.userInfo : null
    },
    detail () {
      const selectedAccount = this.$store.state.collection && this.$store.state.collection.selectedAccount
      return {
        customerId: selectedAccount && selectedAccount.customerId ? selectedAccount.customerId : '--',
        customerName: selectedAccount && selectedAccount.customerName ? selectedAccount.customerName : '--',
        productName: selectedAccount && selectedAccount.productName ? selectedAccount.productName : '--',
        overdueAmount: selectedAccount && selectedAccount.overdueAmount ? selectedAccount.overdueAmount : '--',
        overdueDays: Number(selectedAccount && selectedAccount.overdueDays ? selectedAccount.overdueDays : 0),
        lastCallDate: selectedAccount && selectedAccount.lastCallDate ? selectedAccount.lastCallDate : '--'
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
      const customerId = this.detail.customerId
      return this.$store.getters['collection/getCollectionRecordsByCustomerId'](customerId)
    },
    creditRecords () {
      return this.$store.getters['collection/getCreditRecordsByCustomerId'](this.detail.customerId)
    },
    smsTemplateContent () {
      return `尊敬的${this.detail.customerName}，您的贷款账户${this.detail.customerId}已临近到期，请尽快处理还款。`
    }
  },
  methods: {
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
    async sendSmsCollect () {
      if (!this.detail.customerId || this.detail.customerId === '--') {
        Message.warning('未识别到客户信息，无法发送短信')
        return
      }
      this.smsLoading = true
      try {
        const { remoteSuccess } = await this.$store.dispatch('collection/sendSmsCollection', {
          customerId: this.detail.customerId,
          content: this.smsTemplateContent
        })
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
      Message.success('催收记录登记成功')
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
</style>
