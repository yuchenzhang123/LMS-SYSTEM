<template>
  <div class="hierarchy-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>机构层级管理</span>
        <el-button v-if="isAdmin" size="small" icon="el-icon-plus" type="primary" @click="openAddJurisdiction">新增管辖机构</el-button>
      </div>

      <div v-loading="loading">
        <el-tree
          v-if="treeData.length > 0"
          :data="treeData"
          :props="treeProps"
          node-key="nodeKey"
          default-expand-all
          class="org-tree"
        >
          <div class="tree-node" slot-scope="{ node, data }">
            <span class="node-label">
              <el-tag :type="data.type === 'manager' ? 'warning' : 'success'" size="mini" style="margin-right: 6px;">
                {{ data.type === 'manager' ? '管辖机构' : '业务机构' }}
              </el-tag>
              <span class="node-code">{{ data.type === 'manager' ? data.orgCode : data.branchCode }}</span>
              <span class="node-name">{{ data.type === 'manager' ? data.orgName : data.branchName }}</span>
            </span>
            <span class="node-actions">
              <el-button type="text" size="mini" icon="el-icon-edit" @click.stop="openEdit(data)">编辑</el-button>
              <el-button v-if="data.type === 'manager'" type="text" size="mini" icon="el-icon-plus" @click.stop="openAddBranch(data)">添加业务机构</el-button>
              <el-button type="text" size="mini" icon="el-icon-delete" class="btn-danger" @click.stop="confirmDelete(data)">删除</el-button>
            </span>
          </div>
        </el-tree>

        <div v-else class="empty-hint">
          暂无机构数据，点击右上角「新增管辖机构」开始构建机构树
        </div>
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="480px"
      @close="resetDialog"
    >
      <el-form :model="form" :rules="rules" ref="addForm" label-width="90px">
        <el-form-item :label="codeLabel" prop="code">
          <el-input
            v-model="form.code"
            :placeholder="codePlaceholder"
            :disabled="isEdit"
            @blur="onCodeBlur"
            clearable
          >
            <el-button v-if="!isEdit" slot="append" icon="el-icon-search" :loading="lookupLoading" @click="onCodeBlur">查询</el-button>
          </el-input>
          <transition name="lookup-fade">
            <div v-if="lookupResult && !isEdit" class="lookup-hint" :class="lookupResult.found ? 'hint-found' : 'hint-notfound'">
              <i :class="lookupResult.found ? 'el-icon-circle-check' : 'el-icon-circle-close'"></i>
              {{ lookupResult.found ? `GBase中找到：${lookupResult.orgName}` : 'GBase中未找到该机构号' }}
            </div>
          </transition>
        </el-form-item>
        <el-form-item label="机构名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入机构名称" clearable></el-input>
        </el-form-item>
        <el-form-item v-if="dialogMode === 'branch'" label="管辖行机构号">
          <el-input :value="form.parentOrgCode" disabled></el-input>
        </el-form-item>
        <el-form-item v-if="dialogMode === 'branch'" label="管辖行机构名称">
          <el-input :value="form.parentOrgName" disabled></el-input>
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getOrgTreeApi,
  addJurisdictionApi,
  updateJurisdictionApi,
  addBranchApi,
  updateBranchApi,
  deleteJurisdictionApi,
  deleteBranchApi,
  lookupOrgInGbaseApi
} from '@/api/org'
import { Message, MessageBox } from 'element-ui'

export default {
  name: 'OrgHierarchy',
  data () {
    return {
      loading: false,
      treeData: [],
      treeProps: { children: 'children', label: 'label' },
      dialogVisible: false,
      dialogMode: 'jurisdiction',
      isEdit: false,
      form: { code: '', name: '', parentOrgCode: '', parentOrgName: '' },
      rules: {
        code: [{ required: true, message: '请输入机构号', trigger: 'blur' }],
        name: [{ required: true, message: '请输入机构名称', trigger: 'blur' }]
      },
      lookupLoading: false,
      lookupResult: null,
      submitting: false
    }
  },
  computed: {
    isAdmin () { return this.$store.state.permission.userRole === 'admin' },
    dialogTitle () {
      if (this.isEdit) return this.dialogMode === 'jurisdiction' ? '编辑管辖机构' : '编辑业务机构'
      return this.dialogMode === 'jurisdiction' ? '新增管辖机构' : '新增业务机构'
    },
    codeLabel () { return this.dialogMode === 'jurisdiction' ? '管辖行号' : '分支行号' },
    codePlaceholder () { return this.dialogMode === 'jurisdiction' ? '请输入管辖行号' : '请输入分支行号' }
  },
  created () { this.loadTree() },
  methods: {
    async loadTree () {
      this.loading = true
      try {
        const res = await getOrgTreeApi()
        const raw = res.data || res || []
        this.treeData = raw.map(j => ({
          ...j,
          nodeKey: 'org_' + j.orgCode,
          children: (j.children || []).map(b => ({
            ...b,
            nodeKey: 'branch_' + b.branchCode + '_' + b.orgCode
          }))
        }))
      } catch (e) {
        Message.error('获取机构树失败')
      } finally {
        this.loading = false
      }
    },
    openAddJurisdiction () {
      this.dialogMode = 'jurisdiction'
      this.isEdit = false
      this.dialogVisible = true
    },
    openAddBranch (jurisdictionNode) {
      this.dialogMode = 'branch'
      this.isEdit = false
      this.form.parentOrgCode = jurisdictionNode.orgCode
      this.form.parentOrgName = jurisdictionNode.orgName
      this.dialogVisible = true
    },
    openEdit (data) {
      this.dialogMode = data.type === 'manager' ? 'jurisdiction' : 'branch'
      this.isEdit = true
      this.form.code = data.type === 'manager' ? data.orgCode : data.branchCode
      this.form.name = data.type === 'manager' ? data.orgName : data.branchName
      this.form.parentOrgCode = data.type === 'staff' ? data.orgCode : ''
      if (data.type === 'staff' && data.orgCode) {
        const parent = this.treeData.find(j => j.orgCode === data.orgCode)
        this.form.parentOrgName = parent ? parent.orgName : ''
      } else {
        this.form.parentOrgName = ''
      }
      this.dialogVisible = true
    },
    async onCodeBlur () {
      if (this.isEdit) return
      const code = this.form.code.trim()
      if (!code) { this.lookupResult = null; return }
      this.lookupResult = null
      this.lookupLoading = true
      try {
        const res = await lookupOrgInGbaseApi(code)
        this.lookupResult = res.data || res
        if (this.lookupResult && this.lookupResult.found && this.lookupResult.orgName && !this.form.name) {
          this.form.name = this.lookupResult.orgName
        }
      } catch (e) {
        this.lookupResult = { found: false }
      } finally {
        this.lookupLoading = false
      }
    },
    async submitForm () {
      const valid = await this.$refs.addForm.validate().catch(() => false)
      if (!valid) return

      // GBase 未找到时二次确认
      if (!this.isEdit && this.lookupResult && !this.lookupResult.found) {
        try {
          await MessageBox.confirm(
            `GBase中未找到机构号「${this.form.code.trim()}」，确定继续${this.isEdit ? '更新' : '新增'}吗？`,
            '确认操作',
            { type: 'warning', confirmButtonText: '继续', cancelButtonText: '取消' }
          )
        } catch (e) { return }
      }

      this.submitting = true
      try {
        const code = this.form.code.trim()
        const name = this.form.name.trim()
        if (this.dialogMode === 'jurisdiction') {
          if (this.isEdit) {
            await updateJurisdictionApi(code, name)
            Message.success('更新管辖机构成功')
          } else {
            await addJurisdictionApi(code, name)
            Message.success('新增管辖机构成功')
          }
        } else {
          const orgCode = this.form.parentOrgCode
          if (this.isEdit) {
            await updateBranchApi(code, orgCode, name)
            Message.success('更新业务机构成功')
          } else {
            await addBranchApi(code, name, orgCode)
            Message.success('新增业务机构成功')
          }
        }
        this.dialogVisible = false
        await this.loadTree()
      } catch (e) {
        Message.error('操作失败：' + (e.message || '未知错误'))
      } finally {
        this.submitting = false
      }
    },
    confirmDelete (node) {
      const isJurisdiction = node.type === 'manager'
      const code = isJurisdiction ? node.orgCode : node.branchCode
      const name = isJurisdiction ? node.orgName : node.branchName
      const tip = isJurisdiction ? '（同时删除其下所有业务机构）' : '（仅从当前管辖行下移除）'
      MessageBox.confirm(
        `确定删除「${name}（${code}）」吗？${tip}`,
        '删除确认',
        { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          if (isJurisdiction) {
            await deleteJurisdictionApi(code)
          } else {
            await deleteBranchApi(code, node.orgCode)
          }
          Message.success('删除成功')
          await this.loadTree()
        } catch (e) {
          Message.error('删除失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    resetDialog () {
      this.form = { code: '', name: '', parentOrgCode: '', parentOrgName: '' }
      this.isEdit = false
      this.lookupResult = null
      this.$refs.addForm && this.$refs.addForm.resetFields()
    }
  }
}
</script>

<style scoped>
.hierarchy-container { padding: 10px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }

.org-tree {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 0;
}
.org-tree >>> .el-tree-node__content { height: 40px; }

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 12px;
}
.node-label { display: flex; align-items: center; gap: 6px; }
.node-code { font-family: monospace; color: #606266; font-size: 13px; }
.node-name { color: #303133; font-size: 13px; }
.node-actions { display: flex; gap: 4px; opacity: 0; transition: opacity 0.15s; }
.tree-node:hover .node-actions { opacity: 1; }
.btn-danger { color: #F56C6C !important; }
.empty-hint { text-align: center; color: #c0c4cc; padding: 40px 0; font-size: 14px; }

.lookup-hint { font-size: 12px; margin-top: 4px; line-height: 1.4; }
.hint-found { color: #67C23A; }
.hint-notfound { color: #F56C6C; }

.lookup-fade-enter-active { animation: lookup-pop 0.2s ease-out; }
@keyframes lookup-pop {
  from { opacity: 0; transform: translateY(-4px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
