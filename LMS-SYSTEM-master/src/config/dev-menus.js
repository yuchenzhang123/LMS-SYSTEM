// src/config/dev-menus.js

// 分支行业务员菜单
export const STAFF_MENUS = [
  {
    modelId: 'collection_root',
    modelName: '个贷催收系统',
    modelUrl: '/collection',
    parameter: 'el-icon-s-finance',
    children: [
      {
        modelId: 'account_list',
        modelName: '个贷账户清单',
        modelUrl: '/collection/account-list',
        children: []
      }
    ]
  },
  {
    modelId: 'insight',
    modelName: '业务洞察',
    modelUrl: '/insight',
    parameter: 'el-icon-data-analysis',
    children: []
  }
]

// 管辖行管理员菜单（账户总览）
export const MANAGER_MENUS = [
  {
    modelId: 'admin_root',
    modelName: '管辖行管理',
    modelUrl: '/admin',
    parameter: 'el-icon-office-building',
    children: [
      {
        modelId: 'admin_account_list',
        modelName: '账户总览',
        modelUrl: '/admin/account-list',
        children: []
      }
    ]
  },
  {
    modelId: 'knowledge',
    modelName: '知识库管理',
    modelUrl: '/knowledge/index',
    parameter: 'el-icon-collection',
    children: []
  },
  {
    modelId: 'insight',
    modelName: '业务洞察',
    modelUrl: '/insight',
    parameter: 'el-icon-data-analysis',
    children: []
  }
]

// 系统管理员菜单（管辖行管理 + 机构管理 + 业务洞察）
export const ADMIN_MENUS = [
  {
    modelId: 'admin_root',
    modelName: '管辖行管理',
    modelUrl: '/admin',
    parameter: 'el-icon-office-building',
    children: [
      {
        modelId: 'admin_account_list',
        modelName: '账户总览',
        modelUrl: '/admin/account-list',
        children: []
      }
    ]
  },
  {
    modelId: 'org_root',
    modelName: '机构管理',
    modelUrl: '/org',
    parameter: 'el-icon-setting',
    children: [
      {
        modelId: 'org_hierarchy',
        modelName: '机构层级管理',
        modelUrl: '/org/hierarchy',
        children: []
      }
    ]
  },
  {
    modelId: 'knowledge',
    modelName: '知识库管理',
    modelUrl: '/knowledge/index',
    parameter: 'el-icon-collection',
    children: []
  },
  {
    modelId: 'insight',
    modelName: '业务洞察',
    modelUrl: '/insight',
    parameter: 'el-icon-data-analysis',
    children: []
  }
]

// 兼容旧引用
export const ALL_DEV_MENUS = ADMIN_MENUS
