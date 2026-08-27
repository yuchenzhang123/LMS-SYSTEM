<template>
  <div class="hierarchy-container">
    <div class="toolbar-row">
      <span class="page-title">机构范围管理</span>
      <el-button v-if="isAdmin" size="small" icon="el-icon-plus" type="primary" @click="openAddGroup">新建范围组</el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="groups.length === 0 && !loading" description="暂无范围组数据，点击右上角「新建范围组」开始构建" :image-size="60" />

      <group-card
        v-for="g in groups"
        :key="g.groupCode"
        :group="g"
        @edit="openEditGroup"
        @add-member="openAddMember"
        @add-manager="openAddManager"
        @changed="loadGroups"
      />
    </div>

    <group-dialog :visible.sync="groupDialogVisible" :group="editGroup" @success="loadGroups" />
    <member-dialog :visible.sync="memberDialogVisible" :group-code="memberGroupCode" @success="loadGroups" />
    <manager-dialog :visible.sync="managerDialogVisible" :group-code="managerGroupCode" @success="loadGroups" />
  </div>
</template>

<script>
import { Message } from 'element-ui'
import { getGroupTreeApi } from '@/api/org'
import GroupCard from '@/components/org/GroupCard.vue'
import GroupDialog from '@/components/org/GroupDialog.vue'
import MemberDialog from '@/components/org/MemberDialog.vue'
import ManagerDialog from '@/components/org/ManagerDialog.vue'

export default {
  name: 'OrgHierarchy',
  components: { GroupCard, GroupDialog, MemberDialog, ManagerDialog },
  data () {
    return {
      loading: false,
      groups: [],

      groupDialogVisible: false,
      editGroup: null,

      memberDialogVisible: false,
      memberGroupCode: '',

      managerDialogVisible: false,
      managerGroupCode: ''
    }
  },
  computed: {
    isAdmin () { return this.$store.state.permission.userRole === 'admin' }
  },
  created () { this.loadGroups() },
  methods: {
    async loadGroups () {
      this.loading = true
      try {
        const res = await getGroupTreeApi()
        this.groups = (res.data || res || []).map(g => ({
          ...g,
          members: g.members || [],
          managers: g.managers || []
        }))
      } catch (e) {
        Message.error('获取范围组列表失败')
      } finally {
        this.loading = false
      }
    },

    openAddGroup () {
      this.editGroup = null
      this.groupDialogVisible = true
    },
    openEditGroup (group) {
      this.editGroup = group
      this.groupDialogVisible = true
    },
    openAddMember (group) {
      this.memberGroupCode = group.groupCode
      this.memberDialogVisible = true
    },
    openAddManager (group) {
      this.managerGroupCode = group.groupCode
      this.managerDialogVisible = true
    }
  }
}
</script>

<style scoped>
.hierarchy-container { padding: 10px; }
.toolbar-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 16px; font-weight: 600; color: #303133; }
</style>
