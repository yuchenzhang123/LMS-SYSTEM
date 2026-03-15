<template>
  <div class="notice-detail-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>通知详情</span>
        <el-button type="text" icon="el-icon-back" @click="goBackToNoticeList">返回消息列表</el-button>
      </div>

      <el-empty v-if="!notice" description="暂无可查看的通知，请先从消息列表进入"></el-empty>

      <div v-else>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="通知标题">{{ notice.title }}</el-descriptions-item>
          <el-descriptions-item label="提醒等级">
            <el-tag :type="levelTagType">{{ levelText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="客户号">{{ notice.customerId }}</el-descriptions-item>
          <el-descriptions-item label="客户姓名">{{ notice.customerName }}</el-descriptions-item>
          <el-descriptions-item label="产品名称">{{ notice.productName }}</el-descriptions-item>
          <el-descriptions-item label="到期日期">{{ notice.dueDate }}</el-descriptions-item>
          <el-descriptions-item label="推送时间">{{ notice.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="最后催收时间">{{ notice.lastCallDate }}</el-descriptions-item>
          <el-descriptions-item label="通知内容" :span="2">{{ notice.message }}</el-descriptions-item>
        </el-descriptions>

        <div class="action-row">
          <el-button type="primary" @click="goAccountDetail">查看账户详情</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { mapState } from 'vuex'

export default {
  name: 'NoticeDetail',
  computed: {
    ...mapState('collection', ['selectedNotice']),
    notice () {
      return this.selectedNotice
    },
    levelTagType () {
      if (!this.notice) return 'info'
      if (this.notice.level === 'high') return 'danger'
      if (this.notice.level === 'medium') return 'warning'
      return 'success'
    },
    levelText () {
      if (!this.notice) return '--'
      if (this.notice.level === 'high') return '高'
      if (this.notice.level === 'medium') return '中'
      return '低'
    }
  },
  methods: {
    goBackToNoticeList () {
      this.$router.push('/notice/list')
    },
    goAccountDetail () {
      if (!this.notice) {
        return
      }
      this.$store.dispatch('collection/setSelectedAccount', {
        source: 'notice',
        account: {
          customerId: this.notice.customerId,
          customerName: this.notice.customerName,
          productName: this.notice.productName,
          overdueAmount: this.notice.overdueAmount,
          overdueDays: this.notice.overdueDays,
          lastCallDate: this.notice.lastCallDate
        }
      })
      this.$router.push('/collection/account-detail')
    }
  }
}
</script>

<style scoped>
.notice-detail-container {
  padding: 10px;
}
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.action-row {
  margin-top: 20px;
  text-align: right;
}
</style>
