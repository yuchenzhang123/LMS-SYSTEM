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
  }
]

// 管辖行管理员菜单（可看账户总览，不可进机构管理）
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
  }
]

// 系统管理员菜单（管辖行管理 + 机构管理）
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
  }
]

// 兼容旧引用
export const ALL_DEV_MENUS = ADMIN_MENUS
