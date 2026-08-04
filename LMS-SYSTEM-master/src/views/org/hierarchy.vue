<template>
  <div class="hierarchy-container">
    <div class="toolbar-row">
      <span class="page-title">机构范围管理</span>
      <el-button v-if="isAdmin" size="small" icon="el-icon-plus" type="primary" @click="openAddGroup">新建范围组</el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="groups.length === 0 && !loading" description="暂无范围组数据，点击右上角「新建范围组」开始构建" :image-size="60" />

      <el-card v-for="g in groups" :key="g.groupCode" shadow="never" class="group-card">
        <!-- 卡片头 -->
        <div class="card-header">
          <span class="group-name">🏢 {{ g.groupName }}</span>
          <span class="group-actions">
            <el-button type="text" size="mini" icon="el-icon-edit" @click="openEditGroup(g)">编辑</el-button>
            <el-button type="text" size="mini" icon="el-icon-delete" class="btn-danger" @click="confirmDeleteGroup(g)">删除</el-button>
          </span>
        </div>

        <!-- ⭐ 管辖机构 -->
        <div class="section">
          <div class="section-title">⭐ 管辖机构 <span class="section-desc">（用这些机构号登录可查看全组数据）</span></div>
          <div class="member-grid">
            <div v-for="m in managerOrgs(g)" :key="m.orgCode" class="member-item manager">
              <span class="member-code">{{ m.orgCode }}</span>
              <span class="member-name">{{ m.orgName }}</span>
              <el-tag size="mini" type="warning">管辖机构</el-tag>
              <el-button type="text" size="mini" class="btn-danger" @click="unsetManagerOrg(g, m)">取消管辖</el-button>
            </div>
            <div v-if="managerOrgs(g).length === 0" class="empty-section">暂无管辖机构</div>
          </div>
        </div>

        <!-- 👤 管理人员 -->
        <div class="section">
          <div class="section-title">
            👤 管理人员 <span class="section-desc">（可绕过自身机构限制查看全组数据）</span>
            <el-button type="text" size="mini" icon="el-icon-plus" @click="openAddManager(g)">添加</el-button>
          </div>
          <div class="member-grid">
            <div v-for="m in g.managers" :key="m.ehrNo" class="member-item">
              <span class="member-code">{{ m.ehrNo }}</span>
              <span class="member-name">{{ m.userName }}</span>
              <el-button type="text" size="mini" class="btn-danger" @click="removeManager(g, m)">移除</el-button>
            </div>
            <div v-if="!g.managers || g.managers.length === 0" class="empty-section">暂无管理人员</div>
          </div>
        </div>

        <!-- 📋 全部机构 -->
        <div class="section">
          <div class="section-title">
            📋 全部机构
            <el-button type="text" size="mini" icon="el-icon-plus" @click="openAddMember(g)">添加</el-button>
          </div>
          <div class="member-grid">
            <div v-for="m in g.members" :key="m.orgCode" class="member-item">
              <span class="member-code">{{ m.orgCode }}</span>
              <span class="member-name">{{ m.orgName }}</span>
              <el-tag v-if="m.isManagerOrg" size="mini" type="warning">管辖机构</el-tag>
              <el-button v-if="!m.isManagerOrg" type="text" size="mini" @click="setAsManager(g, m)">设为管辖</el-button>
              <el-button type="text" size="mini" class="btn-danger" @click="removeMember(g, m)">移除</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- ========== 新建/编辑范围组 ========== -->
    <el-dialog
      :title="groupDialogTitle"
      :visible.sync="groupDialogVisible"
      width="420px"
      @close="resetGroupDialog"
    >
      <el-form :model="groupForm" :rules="groupRules" ref="groupForm" label-width="90px">
        <el-form-item label="范围组名称" prop="groupName">
          <el-input v-model="groupForm.groupName" placeholder="例：海秀支行" clearable />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="groupSubmitting" @click="submitGroupForm">确定</el-button>
      </div>
    </el-dialog>

    <!-- ========== 添加机构 ========== -->
    <el-dialog
      title="添加机构"
      :visible.sync="memberDialogVisible"
      width="450px"
      @close="resetMemberDialog"
    >
      <el-form :model="memberForm" :rules="memberRules" ref="memberForm" label-width="90px">
        <el-form-item label="机构号" prop="orgCode">
          <el-input v-model="memberForm.orgCode" placeholder="请输入机构号" clearable @blur="onMemberCodeBlur">
            <el-button slot="append" icon="el-icon-search" :loading="memberLookupLoading" @click="onMemberCodeBlur">查询</el-button>
          </el-input>
          <transition name="lookup-fade">
            <div v-if="memberLookupResult" class="lookup-hint" :class="memberLookupResult.found ? 'hint-found' : 'hint-notfound'">
              <i :class="memberLookupResult.found ? 'el-icon-circle-check' : 'el-icon-circle-close'" />
              {{ memberLookupResult.found ? `GBase中找到：${memberLookupResult.orgName}` : 'GBase中未找到该机构号' }}
            </div>
          </transition>
        </el-form-item>
        <el-form-item label="机构名称" prop="orgName">
          <el-input v-model="memberForm.orgName" placeholder="请输入机构名称" clearable />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="memberSubmitting" @click="submitMemberForm">确定</el-button>
      </div>
    </el-dialog>

    <!-- ========== 添加管理人员 ========== -->
    <el-dialog
      title="添加管理人员"
      :visible.sync="managerDialogVisible"
      width="480px"
      @close="resetManagerDialog"
    >
      <el-form :model="managerForm" :rules="managerRules" ref="managerForm" label-width="90px">
        <el-form-item label="EHR号" prop="ehrNo">
          <el-input v-model="managerForm.ehrNo" placeholder="请输入人员EHR号" clearable @blur="onManagerEhrBlur">
            <el-button slot="append" icon="el-icon-search" :loading="managerLookupLoading" @click="onManagerEhrBlur">查询</el-button>
          </el-input>
          <transition name="lookup-fade">
            <div v-if="managerLookupResult" class="lookup-hint" :class="managerLookupResult.found ? 'hint-found' : 'hint-notfound'">
              <i :class="managerLookupResult.found ? 'el-icon-circle-check' : 'el-icon-circle-close'" />
              {{ managerLookupResult.found
                  ? `找到：${managerLookupResult.userName || ''} (${managerLookupResult.orgCode || ''})`
                  : '未找到该EHR号对应的人员信息' }}
            </div>
          </transition>
        </el-form-item>
        <el-form-item label="姓名" prop="userName">
          <el-input v-model="managerForm.userName" placeholder="请输入人员姓名" clearable />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="managerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="managerSubmitting" @click="submitManagerForm">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getGroupTreeApi,
  createGroupApi,
  updateGroupApi,
  deleteGroupApi,
  addMemberApi,
  removeMemberApi,
  setManagerOrgApi,
  unsetManagerOrgApi,
  addGroupManagerApi,
  removeGroupManagerApi,
  lookupOrgInGbaseApi,
  userLookupApi
} from '@/api/org'
import { Message, MessageBox } from 'element-ui'

export default {
  name: 'OrgHierarchy',
  data () {
    return {
      loading: false,
      groups: [],

      // 范围组弹窗
      groupDialogVisible: false,
      groupSubmitting: false,
      isGroupEdit: false,
      editGroupCode: '',
      groupForm: { groupName: '' },
      groupRules: {
        groupName: [{ required: true, message: '请输入范围组名称', trigger: 'blur' }]
      },

      // 添加机构弹窗
      memberDialogVisible: false,
      memberSubmitting: false,
      currentGroupCode: '',
      memberForm: { orgCode: '', orgName: '' },
      memberRules: {
        orgCode: [{ required: true, message: '请输入机构号', trigger: 'blur' }],
        orgName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }]
      },
      memberLookupLoading: false,
      memberLookupResult: null,

      // 添加管理人员弹窗
      managerDialogVisible: false,
      managerSubmitting: false,
      managerCurrentGroupCode: '',
      managerForm: { ehrNo: '', userName: '' },
      managerRules: {
        ehrNo: [{ required: true, message: '请输入EHR号', trigger: 'blur' }],
        userName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
      },
      managerLookupLoading: false,
      managerLookupResult: null
    }
  },
  computed: {
    isAdmin () { return this.$store.state.permission.userRole === 'admin' },
    groupDialogTitle () { return this.isGroupEdit ? '编辑范围组' : '新建范围组' }
  },
  created () { this.loadGroups() },
  methods: {
    // ======================== 数据加载 ========================

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

    // 获取管辖机构列表
    managerOrgs (group) {
      return (group.members || []).filter(m => m.isManagerOrg)
    },

    // ======================== 范围组 CRUD ========================

    openAddGroup () {
      this.isGroupEdit = false
      this.editGroupCode = ''
      this.groupDialogVisible = true
    },
    openEditGroup (group) {
      this.isGroupEdit = true
      this.editGroupCode = group.groupCode
      this.groupForm.groupName = group.groupName
      this.groupDialogVisible = true
    },
    async submitGroupForm () {
      const valid = await this.$refs.groupForm.validate().catch(() => false)
      if (!valid) return
      this.groupSubmitting = true
      try {
        if (this.isGroupEdit) {
          await updateGroupApi(this.editGroupCode, this.groupForm.groupName.trim())
          Message.success('更新范围组成功')
        } else {
          await createGroupApi(this.groupForm.groupName.trim())
          Message.success('新建范围组成功')
        }
        this.groupDialogVisible = false
        await this.loadGroups()
      } catch (e) {
        Message.error('操作失败：' + (e.message || '未知错误'))
      } finally {
        this.groupSubmitting = false
      }
    },
    confirmDeleteGroup (group) {
      MessageBox.confirm(
        `确定删除范围组「${group.groupName}」吗？将同时删除其下所有机构成员和管理人员。`,
        '删除确认',
        { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await deleteGroupApi(group.groupCode)
          Message.success('删除成功')
          await this.loadGroups()
        } catch (e) {
          Message.error('删除失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    resetGroupDialog () {
      this.groupForm = { groupName: '' }
      this.isGroupEdit = false
      this.editGroupCode = ''
      this.$refs.groupForm && this.$refs.groupForm.resetFields()
    },

    // ======================== 组成员 ========================

    openAddMember (group) {
      this.currentGroupCode = group.groupCode
      this.memberDialogVisible = true
    },
    async onMemberCodeBlur () {
      const code = this.memberForm.orgCode.trim()
      if (!code) { this.memberLookupResult = null; return }
      this.memberLookupResult = null
      this.memberLookupLoading = true
      try {
        const res = await lookupOrgInGbaseApi(code)
        this.memberLookupResult = res.data || res
        if (this.memberLookupResult && this.memberLookupResult.found && this.memberLookupResult.orgName && !this.memberForm.orgName) {
          this.memberForm.orgName = this.memberLookupResult.orgName
        }
      } catch (e) {
        this.memberLookupResult = { found: false }
      } finally {
        this.memberLookupLoading = false
      }
    },
    async submitMemberForm () {
      const valid = await this.$refs.memberForm.validate().catch(() => false)
      if (!valid) return
      if (this.memberLookupResult && !this.memberLookupResult.found) {
        try {
          await MessageBox.confirm(
            `GBase中未找到机构号「${this.memberForm.orgCode.trim()}」，确定继续添加吗？`,
            '确认操作',
            { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }
          )
        } catch (e) { return }
      }
      this.memberSubmitting = true
      try {
        await addMemberApi(this.currentGroupCode, this.memberForm.orgCode.trim(), this.memberForm.orgName.trim())
        Message.success('添加机构成功')
        this.memberDialogVisible = false
        await this.loadGroups()
      } catch (e) {
        Message.error('添加失败：' + (e.message || '未知错误'))
      } finally {
        this.memberSubmitting = false
      }
    },
    setAsManager (group, member) {
      MessageBox.confirm(
        `确定将「${member.orgName || member.orgCode}」设为管辖机构吗？设为管辖机构后，用该机构号登录可查看全组数据。`,
        '确认操作',
        { type: 'info', confirmButtonText: '确定', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await setManagerOrgApi(group.groupCode, member.orgCode)
          Message.success('设为管辖机构成功')
          await this.loadGroups()
        } catch (e) {
          Message.error('操作失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    unsetManagerOrg (group, member) {
      MessageBox.confirm(
        `确定取消「${member.orgName || member.orgCode}」的管辖机构资格吗？`,
        '确认操作',
        { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await unsetManagerOrgApi(group.groupCode, member.orgCode)
          Message.success('取消管辖机构成功')
          await this.loadGroups()
        } catch (e) {
          Message.error('操作失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    removeMember (group, member) {
      MessageBox.confirm(
        `确定从范围组「${group.groupName}」中移出机构「${member.orgName || member.orgCode}」吗？`,
        '移出确认',
        { type: 'warning', confirmButtonText: '确定移出', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await removeMemberApi(group.groupCode, member.orgCode)
          Message.success('移出成功')
          await this.loadGroups()
        } catch (e) {
          Message.error('移出失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    resetMemberDialog () {
      this.memberForm = { orgCode: '', orgName: '' }
      this.currentGroupCode = ''
      this.memberLookupResult = null
      this.$refs.memberForm && this.$refs.memberForm.resetFields()
    },

    // ======================== 管理人员 ========================

    openAddManager (group) {
      this.managerCurrentGroupCode = group.groupCode
      this.managerDialogVisible = true
    },
    async onManagerEhrBlur () {
      const ehrNo = this.managerForm.ehrNo.trim()
      if (!ehrNo) { this.managerLookupResult = null; return }
      this.managerLookupResult = null
      this.managerLookupLoading = true
      try {
        const res = await userLookupApi(ehrNo)
        this.managerLookupResult = res.data || res
        if (this.managerLookupResult && this.managerLookupResult.found) {
          if (this.managerLookupResult.userName && !this.managerForm.userName) {
            this.managerForm.userName = this.managerLookupResult.userName
          }
        }
      } catch (e) {
        this.managerLookupResult = { found: false }
      } finally {
        this.managerLookupLoading = false
      }
    },
    async submitManagerForm () {
      const valid = await this.$refs.managerForm.validate().catch(() => false)
      if (!valid) return
      if (this.managerLookupResult && !this.managerLookupResult.found) {
        try {
          await MessageBox.confirm(
            `未找到EHR号「${this.managerForm.ehrNo.trim()}」对应的人员信息，确定继续添加吗？`,
            '确认操作',
            { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }
          )
        } catch (e) { return }
      }
      this.managerSubmitting = true
      try {
        await addGroupManagerApi(
          this.managerCurrentGroupCode,
          this.managerForm.ehrNo.trim(),
          this.managerForm.userName.trim()
        )
        Message.success('添加管理人员成功')
        this.managerDialogVisible = false
        await this.loadGroups()
      } catch (e) {
        Message.error('添加失败：' + (e.message || '未知错误'))
      } finally {
        this.managerSubmitting = false
      }
    },
    removeManager (group, manager) {
      MessageBox.confirm(
        `确定移除管理人员「${manager.userName || manager.ehrNo}」吗？`,
        '移除确认',
        { type: 'warning', confirmButtonText: '确定移除', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          await removeGroupManagerApi(group.groupCode, manager.ehrNo)
          Message.success('移除管理人员成功')
          await this.loadGroups()
        } catch (e) {
          Message.error('移除失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    resetManagerDialog () {
      this.managerForm = { ehrNo: '', userName: '' }
      this.managerCurrentGroupCode = ''
      this.managerLookupResult = null
      this.$refs.managerForm && this.$refs.managerForm.resetFields()
    }
  }
}
</script>

<style scoped>
.hierarchy-container { padding: 10px; }
.toolbar-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 16px; font-weight: 600; color: #303133; }

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

.lookup-hint { font-size: 12px; margin-top: 4px; line-height: 1.4; }
.hint-found { color: #67C23A; }
.hint-notfound { color: #F56C6C; }
.lookup-fade-enter-active { animation: lookup-pop 0.2s ease-out; }
@keyframes lookup-pop {
  from { opacity: 0; transform: translateY(-4px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
