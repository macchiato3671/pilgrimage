<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LocationCard from '../components/cards/LocationCard.vue'
import AppIcon from '../components/common/AppIcon.vue'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import LocationDetailPanel from '../components/common/LocationDetailPanel.vue'
import KakaoMap from '../components/map/KakaoMap.vue'
import { usePlanMapOverlay } from '../composables/usePlanMapOverlay'
import { useExploreStore } from '../stores/explore'
import { useUiStore } from '../stores/ui'
import { useWishlistStore } from '../stores/wishlist'

const route = useRoute()
const router = useRouter()
const explore = useExploreStore()
const wishlist = useWishlistStore()
const ui = useUiStore()
const { planOverlayItems, planOverlayColor } = usePlanMapOverlay()
const selected = ref(null)
const detailOpen = ref(false)
const filter = ref('')
const scenes = computed(() => {
  const keyword = filter.value.trim().toLowerCase()
  return keyword ? explore.scenes.filter((scene) => `${scene.name} ${scene.address}`.toLowerCase().includes(keyword)) : explore.scenes
})

const open = async (scene) => {
  selected.value = scene
  detailOpen.value = true
  try { selected.value = await explore.sceneDetail(scene.sceneId) } catch { /* 목록 정보로 표시 */ }
}
const toggle = async (scene) => {
  try { await wishlist.toggle(scene); ui.toast(wishlist.has(scene.sceneId) ? '위시리스트에 담았습니다.' : '위시리스트에서 제거했습니다.', 'success') }
  catch (error) { ui.toast(error.message, 'error') }
}
const nearby = (scene) => { detailOpen.value = false; router.push({ name: 'places', query: { sceneId: scene.sceneId } }) }

onMounted(async () => {
  try { await explore.loadScenes(route.params.dramaId) } catch (error) { ui.toast(error.message, 'error') }
})
</script>

<template>
  <div class="split-page" :class="{ 'detail-open': detailOpen }">
    <section class="content-panel location-list-panel">
      <header class="subpage-header">
        <button class="icon-button" aria-label="뒤로" @click="router.back()"><AppIcon name="back" /></button>
        <div><span class="eyebrow">DRAMA SCENES</span><h1>{{ explore.drama?.title || '촬영지' }}</h1><p>{{ explore.drama?.description }}</p></div>
      </header>
      <label class="inline-search"><AppIcon name="search" :size="17" /><input v-model="filter" placeholder="촬영지 이름이나 주소로 찾기" /></label>
      <LoadingState v-if="explore.scenesLoading" />
      <EmptyState v-else-if="!scenes.length" title="등록된 촬영지가 없습니다." description="작품의 촬영지 데이터가 추가되면 이곳에 표시됩니다." />
      <div v-else class="card-list location-list">
        <LocationCard v-for="scene in scenes" :key="scene.sceneId" :item="scene" :wished="wishlist.has(scene.sceneId)" @select="open" @toggle-wishlist="toggle" />
      </div>
    </section>
    <section class="map-panel">
      <KakaoMap
        :items="explore.scenes"
        :fit-items="explore.scenes"
        :favorites="wishlist.items"
        :overlay-items="planOverlayItems"
        :route-items="planOverlayItems"
        :route-color="planOverlayColor"
        :selected-id="selected?.sceneId"
        connect-items
        @select="open"
      />
    </section>
    <LocationDetailPanel :open="detailOpen" :item="selected" :wished="selected ? wishlist.has(selected.sceneId) : false" @close="detailOpen = false" @toggle-wishlist="toggle" @nearby="nearby" />
  </div>
</template>
