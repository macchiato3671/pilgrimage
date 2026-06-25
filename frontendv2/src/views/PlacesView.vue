<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { dramaApi } from '../api/services'
import LocationCard from '../components/cards/LocationCard.vue'
import AppIcon from '../components/common/AppIcon.vue'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import LocationDetailPanel from '../components/common/LocationDetailPanel.vue'
import SearchBox from '../components/common/SearchBox.vue'
import KakaoMap from '../components/map/KakaoMap.vue'
import { usePlanMapOverlay } from '../composables/usePlanMapOverlay'
import { PLACE_CATEGORIES, SEOUL_CENTER } from '../config/app'
import { usePlacesStore } from '../stores/places'
import { useUiStore } from '../stores/ui'
import { useWishlistStore } from '../stores/wishlist'

const route = useRoute()
const places = usePlacesStore()
const wishlist = useWishlistStore()
const ui = useUiStore()
const { planOverlayItems } = usePlanMapOverlay()
const query = ref(String(route.query.keyword || ''))
const category = ref(PLACE_CATEGORIES[0])
const center = ref({ ...SEOUL_CENTER })
const sourceScene = ref(null)
const selected = ref(null)
const detailOpen = ref(false)
const sceneId = computed(() => route.query.sceneId || null)
const mapItems = computed(() => (sourceScene.value ? [sourceScene.value, ...places.mapItems] : places.mapItems))
const mapFitItems = computed(() => (places.mapItems.length ? places.mapItems : sourceScene.value ? [sourceScene.value] : []))
const resultTitle = computed(() => (query.value.trim() ? '키워드 검색' : category.value.label))
const selectedMapId = computed(() => (selected.value ? `${selected.value.kind}:${selected.value.sceneId ?? selected.value.placeId}` : ''))

const load = async (page = 0, { forceMap = false } = {}) => {
  const typedKeyword = query.value.trim()
  const isKeywordSearch = Boolean(typedKeyword)
  try {
    await places.search({
      keyword: isKeywordSearch ? typedKeyword : category.value.keyword,
      category: category.value.id,
      center: center.value,
      sceneId: sceneId.value,
      page,
      useCenter: forceMap || !isKeywordSearch,
      useCategory: !isKeywordSearch,
    })
  } catch (error) { ui.toast(error.message, 'error') }
}
const selectCategory = (next) => { category.value = next; query.value = ''; load() }
const open = async (item) => {
  selected.value = item
  if (item.latitude != null && item.longitude != null) center.value = { latitude: item.latitude, longitude: item.longitude }
  detailOpen.value = true
  try { selected.value = item.kind === 'scene' ? await dramaApi.scene(item.sceneId) : await places.detail(item.placeId) } catch { /* 카드 데이터 유지 */ }
}
const toggle = async (scene) => {
  try { await wishlist.toggle(scene); ui.toast(wishlist.has(scene.sceneId) ? '위시리스트에 담았습니다.' : '위시리스트에서 제거했습니다.', 'success') }
  catch (error) { ui.toast(error.message, 'error') }
}

const loadSourceScene = async () => {
  sourceScene.value = null
  if (sceneId.value) {
    try {
      sourceScene.value = await dramaApi.scene(sceneId.value)
      if (sourceScene.value.latitude != null) center.value = { latitude: sourceScene.value.latitude, longitude: sourceScene.value.longitude }
    } catch (error) { ui.toast(`기준 촬영지를 불러오지 못했습니다: ${error.message}`, 'error') }
  }
}

onMounted(async () => {
  await loadSourceScene()
  load()
})
watch(() => route.query.sceneId, async () => {
  await loadSourceScene()
  load()
})
</script>

<template>
  <div class="split-page" :class="{ 'detail-open': detailOpen }">
    <section class="content-panel location-list-panel">
      <header class="page-heading compact-heading">
        <span class="eyebrow">PLACES NEAR THE SCENE</span>
        <h1>{{ sourceScene ? `‘${sourceScene.name}’ 주변 장소` : '여행 장소 찾기' }}</h1>
        <p>{{ sourceScene ? '촬영지와 함께 둘러보기 좋은 장소를 찾아보세요.' : '키워드·카테고리·현재 지도 위치를 조합해 검색할 수 있습니다.' }}</p>
      </header>
      <SearchBox v-model="query" placeholder="장소 이름 또는 키워드" :busy="places.loading" @search="load()" />
      <div class="category-tabs">
        <button v-for="item in PLACE_CATEGORIES" :key="`${item.label}-${item.id}`" :class="{ active: category.label === item.label }" @click="selectCategory(item)">{{ item.label }}</button>
      </div>
      <div class="section-heading"><div><span class="eyebrow">RESULT</span><h2>{{ resultTitle }}</h2></div><button v-if="!sourceScene" class="text-button" @click="load(0, { forceMap: true })"><AppIcon name="pin" :size="15" />현재 지도에서 검색</button></div>
      <LoadingState v-if="places.loading && !places.items.length" />
      <EmptyState v-else-if="!places.items.length" title="검색된 장소가 없습니다." description="검색어 또는 카테고리를 바꿔 보세요." />
      <div v-else class="card-list location-list">
        <LocationCard v-for="place in places.items" :key="place.placeId" :item="place" @select="open" />
        <button v-if="places.hasNext" class="button secondary load-more" :disabled="places.loading" @click="load(places.page + 1)">장소 더 보기</button>
      </div>
    </section>
    <section class="map-panel">
      <KakaoMap
        :items="mapItems"
        :fit-items="mapFitItems"
        :favorites="wishlist.items"
        :overlay-items="planOverlayItems"
        :center="center"
        :selected-id="selectedMapId"
        @select="open"
        @center-change="center = $event"
      />
    </section>
    <LocationDetailPanel :open="detailOpen" :item="selected" :wished="selected?.kind === 'scene' && wishlist.has(selected.sceneId)" @close="detailOpen = false" @toggle-wishlist="toggle" />
  </div>
</template>
