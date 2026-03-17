// 根据环境变量动态配置代理
const getProxyConfig = () => {
  const ssoTarget = process.env.VUE_APP_SSO_TARGET || process.env.VUE_APP_SSO_API_URL
  const apiTarget = process.env.VUE_APP_API_TARGET || process.env.VUE_APP_API_BASE_URL
  const ssoPrefix = process.env.VUE_APP_SSO_PREFIX || '/ssoservice'
  const apiPrefix = process.env.VUE_APP_API_PREFIX || '/api'

  return {
    [ssoPrefix]: {
      target: ssoTarget,
      changeOrigin: true,
      pathRewrite: {
        [`^${ssoPrefix}`]: ''
      }
    },
    [apiPrefix]: {
      target: apiTarget,
      changeOrigin: true,
      pathRewrite: {
        [`^${apiPrefix}`]: ''
      }
    }
  }
}

const devHost = process.env.VUE_APP_DEV_HOST || '0.0.0.0'
const devPort = Number(process.env.VUE_APP_DEV_PORT || 8080)

module.exports = {
  runtimeCompiler: true, // 开启此项以支持 template 字符串渲染
  devServer: {
    host: devHost,
    port: devPort,
    // 如果还是报错，尝试关闭 host 检查（仅限开发环境）
    disableHostCheck: true,
    proxy: getProxyConfig()
  }
}
