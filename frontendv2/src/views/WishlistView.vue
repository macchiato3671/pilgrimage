<script setup>
import { computed, ref, watchEffect } from 'vue'
import LocationCard from '../components/cards/LocationCard.vue'
import AppIcon from '../components/common/AppIcon.vue'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import LocationDetailModal from '../components/common/LocationDetailModal.vue'
import KakaoMap from '../components/map/KakaoMap.vue'
import { dramaApi } from '../api/services'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'
import { useWishlistStore } from '../stores/wishlist'

const auth = useAuthStore()
const wishlist = useWishlistStore()
const ui = useUiStore()
const selectedGroup = ref(null)
const selected = ref(null)
const detailOpen = ref(false)
const group = computed(() => wishlist.grouped.find((item) => String(item.dramaId || item.title) === String(selectedGroup.value)) || wishlist.grouped[0])
watchEffect(() => {
  if (!selectedGroup.value && wishlist.grouped.length) selectedGroup.value = wishlist.grouped[0].dramaId || wishlist.grouped[0].title
})
const open = async (scene) => {
  selected.value = scene
  detailOpen.value = true
  try { selected.value = await dramaApi.scene(scene.sceneId) } catch { /* 목록 데이터 유지 */ }
}
const toggle = async (scene) => {
  try { await wishlist.toggle(scene); ui.toast(wishlist.has(scene.sceneId) ? '위시리스트에 담았습니다.' : '위시리스트에서 제거했습니다.', 'success') }
  catch (error) { ui.toast(error.message, 'error') }
}
</script>

<template>
  <div class="split-page wishlist-page">
    <section class="content-panel wishlist-panel">
      <header class="page-heading compact-heading"><span class="eyebrow">MY PILGRIMAGE</span><h1>위시리스트</h1><p>가 보고 싶은 촬영지를 작품별로 모아 여행 계획에 추가하세요.</p></header>
      <RouterLink v-if="!auth.isAuthenticated && wishlist.items.length" class="guest-banner" to="/login"><AppIcon name="sync" :size="20" /><span><strong>비회원 저장 중</strong> 로그인하면 이 위시리스트를 계정에 동기화할 수 있습니다.</span><AppIcon name="chevron" :size="17" /></RouterLink>
      <LoadingState v-if="wishlist.loading" />
      <EmptyState v-else-if="!wishlist.items.length" title="아직 저장한 촬영지가 없습니다." description="작품의 촬영지에서 하트 버튼을 눌러 추가해 보세요." icon="heart"><RouterLink class="button primary" to="/dramas">작품 찾으러 가기</RouterLink></EmptyState>
      <template v-else>
        <div class="wishlist-groups">
          <button v-for="item in wishlist.grouped" :key="item.dramaId || item.title" :class="{ active: group === item }" @click="selectedGroup = item.dramaId || item.title"><strong>{{ item.title }}</strong><span>{{ item.scenes.length }}</span></button>
        </div>
        <div class="section-heading"><div><span class="eyebrow">SAVED SCENES</span><h2>{{ group?.title }}</h2></div><span>{{ group?.scenes.length }}개</span></div>
        <div class="card-list location-list"><LocationCard v-for="scene in group?.scenes" :key="scene.sceneId" :item="scene" wished @select="open" @toggle-wishlist="toggle" /></div>
      </template>
    </section>
    <section class="map-panel"><KakaoMap :items="group?.scenes || []" :favorites="wishlist.items" :selected-id="selected?.sceneId" @select="open" /></section>
    <LocationDetailModal :open="detailOpen" :item="selected" :wished="true" @close="detailOpen = false" @toggle-wishlist="toggle" />
  </div>
</template>
