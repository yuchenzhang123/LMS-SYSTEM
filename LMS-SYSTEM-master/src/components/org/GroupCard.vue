<template>
  <el-card shadow="never" class="group-card">
    <!-- 卡片头 -->
    <div class="card-header">
      <span class="group-name">🏢 {{ group.groupName }}</span>
      <span class="group-actions">
        <el-button type="text" size="mini" icon="el-icon-edit" @click="openEditGroup">编辑</el-button>
        <el-button type="text" size="mini" icon="el-icon-delete" class="btn-danger" @click="confirmDeleteGroup">删除</el-button>
      </span>
    </div>

    <!-- ⭐ 管辖机构 -->
    <div class="section">
      <div class="section-title">⭐ 管辖机构 <span class="section-desc">（用这些机构号登录可查看全组数据）</span></div>
      <div class="member-grid">
        <div v-for="m in managerOrgs" :key="m.orgCode" class="member-item manager">
          <span class="member-code">{{ m.orgCode }}</span>
          <span class="member-name">{{ m.orgName }}</span>
          <el-tag size="mini" type="warning">管辖机构</el-tag>
          <el-button type="text" size="mini" class="btn-danger" @click="unsetManagerOrg(m)">取消管辖</el-button>
        </div>
        <div v-if="managerOrgs.length === 0" class="empty-section">暂无管辖机构</div>
      </div>
    </div>

    <!-- 👤 管理人员 -->
    <div class="section">
      <div class="section-title">
        👤 管理人员 <span class="section-desc">（可绕过自身机构限制查看全组数据）</span>
        <el-button type="text" size="mini" icon="el-icon-plus" @click="openAddManager">添加</el-button>
      </div>
      <div class="member-grid">
        <div v-for="m in group.managers" :key="m.ehrNo" class="member-item">
          <span class="member-code">{{ m.ehrNo }}</span>
          <span class="member-name">{{ m.userName }}</span>
          <el-button type="text" size="mini" class="btn-danger" @click="removeManager(m)">移除</el-button>
        </div>
        <div v-if="!group.managers || group.managers.length === 0" class="empty-section">暂无管理人员</div>
      </div>
    </div>

    <!-- 📋 全部机构 -->
    <div class="section">
      <div class="section-title">
        📋 全部机构
        <el-button type="text" size="mini" icon="el-icon-plus" @click="openAddMember">添加</el-button>
      </div>
      <div class="member-grid">
        <div v-for="m in group.members" :key="m.orgCode" class="member-item">
          <span class="member-code">{{ m.orgCode }}</span>
          <span class="member-name">{{ m.orgName }}</span>
          <el-tag v-if="m.isManagerOrg" size="mini" type="warning">管辖机构</el-tag>
          <el-button v-if="!m.isManagerOrg" type="text" size="mini" @click="setAsManager(m)">设为管辖</el-button>
          <el-button type="text" size="mini" class="btn-danger" @click="removeMember(m)">移除</el-button>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
import { Message, MessageBox } from 'element-ui'
import {
  deleteGroupApi,
  removeMemberApi,
  setManagerOrgApi,
  unsetManagerOrgApi,
  removeGroupManagerApi
} from '@/api/org'

export default {
  name: 'GroupCard',
  props: {
    group: { type: Object, required: true }
  },
  computed: {
    managerOrgs () {
      return (this.group.members || []).filter(m => m.isManagerOrg)
    }
  },
  methods: {
    // ---- 打开弹窗（交给父组件） ----
    openEditGroup () { this.$emit('edit', this.group) },
    openAddMember () { this.$emit('add-member', this.group) },
    openAddManager () { this.$emit('add-manager', this.group) },

    // ---- 直接操作（自治，成功后通知父刷新） ----
    confirmDeleteGroup () {
      MessageBox.confirm(
        `确定删除范围组「${this.group.groupName}」吗？将同时删除其下所有机构成员和管理人员。`,
        '删除确认',
        { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await deleteGroupApi(this.group.groupCode)
          Message.success('删除成功')
          this.$emit('changed')
        } catch (e) {
          Message.error('删除失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    setAsManager (member) {
      MessageBox.confirm(
        `确定将「${member.orgName || member.orgCode}」设为管辖机构吗？设为管辖机构后，用该机构号登录可查看全组数据。`,
        '确认操作',
        { type: 'info', confirmButtonText: '确定', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await setManagerOrgApi(this.group.groupCode, member.orgCode)
          Message.success('设为管辖机构成功')
          this.$emit('changed')
        } catch (e) {
          Message.error('操作失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    unsetManagerOrg (member) {
      MessageBox.confirm(
        `确定取消「${member.orgName || member.orgCode}」的管辖机构资格吗？`,
        '确认操作',
        { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await unsetManagerOrgApi(this.group.groupCode, member.orgCode)
          Message.success('取消管辖机构成功')
          this.$emit('changed')
        } catch (e) {
          Message.error('操作失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    removeMember (member) {
      MessageBox.confirm(
        `确定从范围组「${this.group.groupName}」中移出机构「${member.orgName || member.orgCode}」吗？`,
        '移出确认',
        { type: 'warning', confirmButtonText: '确定移出', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await removeMemberApi(this.group.groupCode, member.orgCode)
          Message.success('移出成功')
          this.$emit('changed')
        } catch (e) {
          Message.error('移出失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    removeManager (manager) {
      MessageBox.confirm(
        `确定移除管理人员「${manager.userName || manager.ehrNo}」吗？`,
        '移除确认',
        { type: 'warning', confirmButtonText: '确定移除', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await removeGroupManagerApi(this.group.groupCode, manager.ehrNo)
          Message.success('移除管理人员成功')
          this.$emit('changed')
        } catch (e) {
          Message.error('移除失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.group-card { margin-bottom: 16px; }
.group-card >>> .el-card__body { padding: 16px 20px; }

.card-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 12px; border-bottom: 1px solid #ebeef5; margin-bottom: 12px;
}
.group-name { font-size: 15px; font-weight: 600; color: #303133; }
.group-actions { display: flex; gap: 4px; }

.section { margin-bottom: 14px; }
.section:last-child { margin-bottom: 0; }
.section-title {
  font-size: 13px; font-weight: 600; color: #606266;
  margin-bottom: 8px; display: flex; align-items: center; gap: 8px;
}
.section-desc { font-weight: 400; color: #909399; font-size: 12px; }

.member-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.member-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 12px; border: 1px solid #e8e8e8; border-radius: 4px;
  background: #fafafa; font-size: 13px;
}
.member-item.manager { border-color: #e6a23c; background: #fdf6ec; }
.member-code { font-family: monospace; color: #606266; }
.member-name { color: #303133; max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty-section { color: #c0c4cc; font-size: 12px; padding: 4px 0; }

.btn-danger { color: #F56C6C !important; }
</style>
