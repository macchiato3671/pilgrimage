<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import brandMark from '../assets/images/brand-mark.png'
import { useAuthStore } from '../stores/auth'
import { useEditorStore } from '../stores/editor'
import { usePlansStore } from '../stores/plans'
import { useUiStore } from '../stores/ui'
import { useWishlistStore } from '../stores/wishlist'
import AppIcon from './common/AppIcon.vue'
import ToastStack from './common/ToastStack.vue'
import PlanEditorDock from './plan/PlanEditorDock.vue'

const auth = useAuthStore()
const editor = useEditorStore()
const plans = usePlansStore()
const wishlist = useWishlistStore()
const ui = useUiStore()
const router = useRouter()

const nav = [
  { to: '/dramas', label: '작품·촬영지', icon: 'film' },
  { to: '/places', label: '주변 장소', icon: 'pin' },
  { to: '/wishlist', label: '위시리스트', icon: 'heart' },
  { to: '/plans', label: '여행 계획', icon: 'calendar' },
]

const loadWorkspace = () =>
  Promise.all([wishlist.load(), plans.load()]).catch((error) => ui.toast(`내 작업을 불러오지 못했습니다: ${error.message}`, 'error'))
const goLogin = async () => {
  if (editor.dirty) {
    if (!window.confirm('로그인 전에 현재 일정 편집 내용을 로컬에 저장할까요?')) return
    try { await editor.save() } catch (error) { return ui.toast(error.message, 'error') }
  }
  router.push({ name: 'login', query: { next: router.currentRoute.value.fullPath } })
}
const logout = async () => {
  if (editor.dirty && !window.confirm('저장하지 않은 일정이 있습니다. 로그아웃할까요?')) return
  editor.close()
  auth.logout()
  await loadWorkspace()
  router.push('/dramas')
}
const unauthorized = () => {
  auth.logout()
  editor.close()
  ui.toast('로그인이 만료되었습니다. 다시 로그인해 주세요.', 'error')
  router.push({ name: 'login', query: { next: router.currentRoute.value.fullPath } })
}

onMounted(() => {
  editor.restore()
  loadWorkspace()
  window.addEventListener('pilgrimage:unauthorized', unauthorized)
})
onBeforeUnmount(() => window.removeEventListener('pilgrimage:unauthorized', unauthorized))
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink class="brand" to="/dramas">
        <span class="brand-image"><img :src="brandMark" alt="" /></span>
        <span class="brand-copy"><strong>필그리미지</strong><small>장면이 여행지가 되다</small></span>
      </RouterLink>
      <nav class="main-nav">
        <RouterLink v-for="item in nav" :key="item.to" :to="item.to" :title="item.label">
          <AppIcon :name="item.icon" :size="21" /><span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="sidebar-spacer" />
      <div class="session-card">
        <template v-if="auth.isAuthenticated">
          <RouterLink class="session-profile" to="/profile" title="회원정보">
            <span class="avatar">{{ auth.member?.nickname?.slice(0, 1) || '여' }}</span>
            <span class="session-copy"><strong>{{ auth.member?.nickname || '여행자' }}</strong><small>{{ auth.member?.email }}</small></span>
          </RouterLink>
          <button class="icon-button" title="로그아웃" @click="logout"><AppIcon name="logout" :size="18" /></button>
        </template>
        <button v-else class="login-link session-login-button" @click="goLogin"><AppIcon name="login" :size="19" /><span>로그인</span></button>
      </div>
    </aside>
    <main class="app-main">
      <RouterView />
    </main>
    <PlanEditorDock />
    <ToastStack />
  </div>
</template>
