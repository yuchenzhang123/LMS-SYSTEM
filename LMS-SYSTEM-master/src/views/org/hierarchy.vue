<template>
  <div class="hierarchy-container">
    <el-card shadow="never">
      <div slot="header" class="header-row">
        <span>机构层级管理</span>
        <el-button size="small" icon="el-icon-plus" type="primary" @click="openAddJurisdiction">新增管辖机构</el-button>
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
              <el-tag
                :type="data.type === 'manager' ? 'warning' : 'success'"
                size="mini"
                style="margin-right: 6px;"
              >{{ data.type === 'manager' ? '管辖机构' : '业务机构' }}</el-tag>
              <span class="node-code">{{ data.type === 'manager' ? data.orgCode : data.branchCode }}</span>
              <span class="node-name">{{ data.type === 'manager' ? data.orgName : data.branchName }}</span>
            </span>
            <span class="node-actions">
              <el-button
                v-if="data.type === 'manager'"
                type="text"
                size="mini"
                icon="el-icon-plus"
                @click.stop="openAddBranch(data)"
              >添加业务机构</el-button>
              <el-button
                type="text"
                size="mini"
                icon="el-icon-delete"
                class="btn-danger"
                @click.stop="confirmDelete(data)"
              >删除</el-button>
            </span>
          </div>
        </el-tree>

        <div v-else class="empty-hint">
          暂无机构数据，点击右上角「新增管辖机构」开始构建机构树
        </div>
      </div>
    </el-card>

    <!-- 新增节点弹窗 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      width="480px"
      @close="resetDialog"
    >
      <el-form :model="form" :rules="rules" ref="addForm" label-width="90px">
        <el-form-item label="机构号" prop="code">
          <el-input
            v-model="form.code"
            :placeholder="dialogMode === 'jurisdiction' ? '请输入管辖行号' : '请输入分支行号'"
            @blur="onCodeBlur"
            clearable
          >
            <el-button slot="append" icon="el-icon-search" :loading="lookupLoading" @click="onCodeBlur">查询</el-button>
          </el-input>
          <div v-if="lookupResult" class="lookup-hint" :class="lookupResult.found ? 'hint-found' : 'hint-notfound'">
            <i :class="lookupResult.found ? 'el-icon-check' : 'el-icon-warning'"></i>
            {{ lookupResult.found ? `GBase中找到：${lookupResult.orgName}` : 'GBase中未找到该机构号' }}
          </div>
        </el-form-item>
        <el-form-item label="机构名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入机构名称" clearable></el-input>
        </el-form-item>
        <el-form-item v-if="dialogMode === 'branch'" label="所属管辖行">
          <el-input :value="form.parentOrgCode" disabled></el-input>
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
  addBranchApi,
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
      treeProps: {
        children: 'children',
        label: 'label'
      },
      dialogVisible: false,
      dialogMode: 'jurisdiction', // 'jurisdiction' | 'branch'
      form: {
        code: '',
        name: '',
        parentOrgCode: ''
      },
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
    dialogTitle () {
      return this.dialogMode === 'jurisdiction' ? '新增管辖机构' : '新增业务机构'
    }
  },
  created () {
    this.loadTree()
  },
  methods: {
    async loadTree () {
      this.loading = true
      try {
        const res = await getOrgTreeApi()
        const raw = res.data || res || []
        // 为 el-tree 添加唯一 nodeKey
        this.treeData = raw.map(j => ({
          ...j,
          nodeKey: 'org_' + j.orgCode,
          children: (j.children || []).map(b => ({
            ...b,
            nodeKey: 'branch_' + b.branchCode
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
      this.dialogVisible = true
    },
    openAddBranch (jurisdictionNode) {
      this.dialogMode = 'branch'
      this.form.parentOrgCode = jurisdictionNode.orgCode
      this.dialogVisible = true
    },
    async onCodeBlur () {
      const code = this.form.code.trim()
      if (!code) {
        this.lookupResult = null
        return
      }
      this.lookupLoading = true
      try {
        const res = await lookupOrgInGbaseApi(code)
        this.lookupResult = res.data || res
        // 如果找到且名称未填，自动补全
        if (this.lookupResult && this.lookupResult.found && this.lookupResult.orgName && !this.form.name) {
          this.form.name = this.lookupResult.orgName
        }
      } catch (e) {
        this.lookupResult = null
      } finally {
        this.lookupLoading = false
      }
    },
    async submitForm () {
      this.$refs.addForm.validate(async valid => {
        if (!valid) return
        this.submitting = true
        try {
          if (this.dialogMode === 'jurisdiction') {
            await addJurisdictionApi(this.form.code.trim(), this.form.name.trim())
            Message.success('新增管辖机构成功')
          } else {
            await addBranchApi(this.form.code.trim(), this.form.name.trim(), this.form.parentOrgCode)
            Message.success('新增业务机构成功')
          }
          this.dialogVisible = false
          await this.loadTree()
        } catch (e) {
          Message.error('操作失败：' + (e.message || '未知错误'))
        } finally {
          this.submitting = false
        }
      })
    },
    confirmDelete (node) {
      const isJurisdiction = node.type === 'manager'
      const code = isJurisdiction ? node.orgCode : node.branchCode
      const name = isJurisdiction ? node.orgName : node.branchName
      const tip = isJurisdiction ? '（同时删除其下所有业务机构）' : ''
      MessageBox.confirm(
        `确定删除「${name}（${code}）」吗？${tip}`,
        '删除确认',
        { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
      ).then(async () => {
        try {
          if (isJurisdiction) {
            await deleteJurisdictionApi(code)
          } else {
            await deleteBranchApi(code)
          }
          Message.success('删除成功')
          await this.loadTree()
        } catch (e) {
          Message.error('删除失败：' + (e.message || '未知错误'))
        }
      }).catch(() => {})
    },
    resetDialog () {
      this.form = { code: '', name: '', parentOrgCode: '' }
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
.org-tree >>> .el-tree-node__content {
  height: 40px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 12px;
}
.node-label {
  display: flex;
  align-items: center;
  gap: 6px;
}
.node-code {
  font-family: monospace;
  color: #606266;
  font-size: 13px;
}
.node-name {
  color: #303133;
  font-size: 13px;
}
.node-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.15s;
}
.tree-node:hover .node-actions {
  opacity: 1;
}
.btn-danger {
  color: #F56C6C !important;
}
.empty-hint {
  text-align: center;
  color: #c0c4cc;
  padding: 40px 0;
  font-size: 14px;
}

.lookup-hint {
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.4;
}
.hint-found { color: #67C23A; }
.hint-notfound { color: #E6A23C; }
</style>
