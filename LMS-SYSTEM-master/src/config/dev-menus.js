// src/config/dev-menus.js
export const ALL_DEV_MENUS = [
  {
    modelId: 'collection_root',
    modelName: '个贷催收系统',
    modelUrl: '/collection',
    parameter: 'el-icon-s-finance', // Element UI 图标
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
    modelId: 'report_mgnt',
    modelName: '统计报表',
    modelUrl: '/reports',
    parameter: 'el-icon-pie-chart',
    children: []
  }
]