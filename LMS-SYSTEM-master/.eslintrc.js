module.exports = {
  root: true,
  env: {
    node: true,
    browser: true,
    es6: true
  },
  extends: [
    'plugin:vue/essential',
    'eslint:recommended'
  ],
  parserOptions: {
    // 使用兼容性最好的旧版解析器，解决版本冲突报错
    parser: 'babel-eslint',
    sourceType: 'module',
    ecmaVersion: 2018
  },
  rules: {
    // --- 0 代表关闭，1 代表警告，2 代表报错 ---

    // 1. 允许 console，方便催收系统联调接口
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',

    // 2. 关闭极其烦人的格式规则（改为警告或关闭）
    'space-before-function-paren': 0,    // 不再强制要求函数括号前的空格
    'no-trailing-spaces': 1,            // 行尾空格仅警告，不阻断编译
    'eol-last': 0,                      // 不强制文件末尾空行
    'vue/no-unused-vars': 1,            // 模板中未使用的变量设为警告
    'no-unused-vars': 1,                // JS中未使用的变量设为警告

    // 3. 基础语法纠错
    'semi': [2, 'never'],               // 强制不使用分号 (Standard规范)
    'quotes': [2, 'single'],            // 强制使用单引号
    'comma-dangle': [2, 'never']        // 禁止末尾逗号
  }
}