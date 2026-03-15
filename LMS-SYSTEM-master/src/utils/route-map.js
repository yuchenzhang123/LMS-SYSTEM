// 假设你的业务组件都放在 src/views 下
export function transformMenuToRoutes(menuList) {
  return menuList.map(item => {
    return {
      path: item.modelUrl,
      name: item.modelId,
      // 关键：动态加载组件
      // 假设 modelUrl 为 "/system/user"，映射到 "@/views/system/user/index.vue"
      component: () => import(`@/views${item.modelUrl}/index.vue`),
      meta: {
        title: item.modelName,
        permissions: item.permissions
      },
      children: item.children && item.children.length > 0
                ? transformMenuToRoutes(item.children)
                : []
    }
  })
}