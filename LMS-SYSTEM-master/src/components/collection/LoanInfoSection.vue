<template>
  <div>
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
  </div>
</template>

<script>
export default {
  name: 'LoanInfoSection',
  props: {
    detail: {
      type: Object,
      default: () => ({})
    }
  }
}
</script>

<style scoped>
.danger-text {
  color: #f56c6c;
  font-weight: bold;
}
</style>
