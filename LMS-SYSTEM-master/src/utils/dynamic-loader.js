// 使用 require.context 预扫描 views 目录下的所有 vue 文件
const viewModules = require.context('@/views', true, /\.vue$/)

/**
 * 动态获取组件
 * @param {string} path 菜单中定义的 modelUrl，例如 "collection/account-list"
 */
export function loadView(path) {
  // 1. 统一格式：去掉开头和结尾的斜杠
  let normalizedPath = path.replace(/^\/|\/$/g, '')

  // 2. 定义尝试匹配的物理路径规则 (相对于 @/views)
  const possiblePaths = [
    `./${normalizedPath}.vue`,
    `./${normalizedPath}/index.vue`
  ]

  // 3. 在 context 中查找是否存在该文件
  let foundPath = null
  for (const p of possiblePaths) {
    try {
      viewModules.resolve(p)
      foundPath = p
      break
    } catch (e) {
      // 继续尝试下一个路径
    }
  }

  if (foundPath) {
    // 关键修正：
    // 在 Webpack 环境下，import() 内部必须包含一部分静态路径前缀
    // 这样 Webpack 才能在编译阶段生成对应的 chunk

    // 我们去掉 foundPath 开头的 "./" 得到类似 "collection/account-list.vue"
    const componentPath = foundPath.replace(/^\.\//, '')

    return () => import(`@/views/${componentPath}`)
  }

  // 4. 如果没找到，打印警告并返回预览占位符
  console.warn(`[DynamicLoader] 无法定位文件。尝试路径: ${possiblePaths.join(' 或 ')}`)
  return () => import('@/views/Preview.vue')
}