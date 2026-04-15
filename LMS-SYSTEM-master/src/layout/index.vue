<template>
  <el-container class="app-wrapper">
    <el-aside width="240px" class="sidebar-container">
      <div class="logo-container">
        <i class="el-icon-bank-card"></i>
        <span>个贷催收管理系统</span>
      </div>

      <el-menu
        :default-active="$route.path"
        class="sidebar-menu"
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        :unique-opened="true"
        router
      >
        <el-menu-item index="/dashboard">
          <i class="el-icon-odometer"></i>
          <span slot="title">工作台概览</span>
        </el-menu-item>

        <sidebar-item v-for="menu in menus" :key="menu.modelId" :item="menu" />
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title || '当前页面' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right" v-if="userInfo">
          <span class="notice-btn" @click="openNoticeList">
            <el-badge :value="unreadNoticeCount" :hidden="unreadNoticeCount === 0">
              <i class="el-icon-bell"></i>
            </el-badge>
          </span>
          <el-dropdown trigger="click">
            <span class="user-dropdown">
              {{ userInfo.userName }} <i class="el-icon-arrow-down"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item icon="el-icon-switch-button" @click.native="handleLogout">
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main">
        <transition name="fade-transform" mode="out-in">
          <router-view />
        </transition>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { mapGetters, mapState } from 'vuex'

// 局部递归组件：处理无限层级菜单
const SidebarItem = {
  name: 'SidebarItem',
  props: ['item'],
  template: `
    <div v-if="item">
      <el-submenu v-if="item.children && item.children.length > 0" :index="item.modelId">
        <template slot="title">
          <i :class="item.parameter || 'el-icon-folder'"></i>
          <span>{{ item.modelName }}</span>
        </template>
        <sidebar-item v-for="child in item.children" :key="child.modelId" :item="child" />
      </el-submenu>

      <el-menu-item v-else :index="item.modelUrl">
        <i :class="item.parameter || 'el-icon-document'"></i>
        <span slot="title">{{ item.modelName }}</span>
      </el-menu-item>
    </div>
  `
}

export default {
  name: 'Layout',
  components: { SidebarItem },
  computed: {
    ...mapState('permission', ['menus', 'userInfo']),
    ...mapGetters('collection', ['unreadNoticeCount'])
  },
  methods: {
    openNoticeList() {
      if (this.$route.path !== '/notice/list') {
        this.$router.push('/notice/list')
      }
    },
    handleLogout() {
      this.$confirm('确定注销并退出系统吗?', '安全提示', {
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('permission/logout')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.app-wrapper { height: 100vh; width: 100%; }
.sidebar-container { background-color: #1f2d3d; color: #fff; }
.logo-container {
  height: 60px; line-height: 60px; padding: 0 20px;
  background: #2b3643; font-size: 16px; font-weight: bold;
  display: flex; align-items: center; gap: 10px;
}
.logo-container i { font-size: 20px; color: #409EFF; }

.app-header {
  height: 60px; display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid #e6e6e6; background: #fff; padding: 0 20px;
}
.header-right { display: flex; align-items: center; gap: 20px; }
.user-dropdown { cursor: pointer; color: #606266; font-weight: 500; }
.notice-btn { cursor: pointer; font-size: 18px; color: #606266; padding: 8px; display: inline-flex; align-items: center; }
.notice-badge { cursor: pointer; font-size: 18px; color: #606266; }
.notice-badge i { cursor: pointer; }

.app-main { background: #f5f7f9; padding: 15px; }

/* 路由动画 */
.fade-transform-enter-active, .fade-transform-leave-active { transition: all .3s; }
.fade-transform-enter { opacity: 0; transform: translateX(-20px); }
.fade-transform-leave-to { opacity: 0; transform: translateX(20px); }
</style>
