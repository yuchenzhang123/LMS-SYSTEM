module.exports = {
  runtimeCompiler: true, // 开启此项以支持 template 字符串渲染
  devServer: {
    host: 'dev.hi.bank-of-china.com',
    port: 8080,
    // 关键补充：允许这个域名访问本地开发服务器
    allowedHosts: [
      'dev.hi.bank-of-china.com'
    ],
    // 如果还是报错，尝试关闭 host 检查（仅限开发环境）
    disableHostCheck: true,
    proxy: {
      '/ssoservice': {
        target: 'http://22.156.22.42:8098',
        changeOrigin: true
      },
      '/api': {
        target: 'http://22.156.22.42:8099',
        changeOrigin: true
      }
    }
  }
}