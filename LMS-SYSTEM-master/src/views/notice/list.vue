<template>
  <div class="notice-list-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>消息推送</span>
        <div class="header-actions">
          <el-tag type="danger" size="small" v-if="unreadNoticeCount > 0">未读 {{ unreadNoticeCount }}</el-tag>
          <el-switch
            class="unread-switch"
            v-model="showUnreadOnly"
            active-text="仅看未读"
            inactive-text="全部消息"
            active-color="#409EFF"
          ></el-switch>
        </div>
      </div>

      <el-table :data="tableNotices" border stripe style="width: 100%">
        <el-table-column label="状态" width="80">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.read ? 'info' : 'danger'">
              {{ scope.row.read ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="通知标题" min-width="260"></el-table-column>
        <el-table-column prop="customerId" label="客户号" width="120"></el-table-column>
        <el-table-column label="提醒等级" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" :type="getLevelTagType(scope.row.level)">
              {{ getLevelText(scope.row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dueDate" label="到期日期" width="120"></el-table-column>
        <el-table-column prop="createdAt" label="推送时间" width="170"></el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="openNoticeDetail(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { mapGetters, mapState } from 'vuex'

export default {
  name: 'NoticeList',
  data () {
    return {
      showUnreadOnly: false
    }
  },
  computed: {
    ...mapState('collection', ['notices']),
    ...mapGetters('collection', ['unreadNoticeCount']),
    tableNotices () {
      if (!this.showUnreadOnly) {
        return this.notices
      }
      return this.notices.filter(item => !item.read)
    }
  },
  methods: {
    getLevelTagType (level) {
      if (level === 'high') return 'danger'
      if (level === 'medium') return 'warning'
      return 'success'
    },
    getLevelText (level) {
      if (level === 'high') return '高'
      if (level === 'medium') return '中'
      return '低'
    },
    openNoticeDetail (notice) {
      this.$store.dispatch('collection/openNotice', notice)
      this.$router.push('/notice/detail')
    }
  }
}
</script>

<style scoped>
.notice-list-container {
  padding: 10px;
}
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.unread-switch {
  margin-left: 4px;
}
</style>
