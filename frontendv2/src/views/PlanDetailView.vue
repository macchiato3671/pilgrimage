<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '../components/common/AppIcon.vue'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import LocationDetailPanel from '../components/common/LocationDetailPanel.vue'
import KakaoMap from '../components/map/KakaoMap.vue'
import PlanFormModal from '../components/plan/PlanFormModal.vue'
import { usePlanMapOverlay } from '../composables/usePlanMapOverlay'
import { SEOUL_CENTER } from '../config/app'
import { formatDate, daysBetween } from '../models/date'
import { useEditorStore } from '../stores/editor'
import { usePlansStore } from '../stores/plans'
import { useUiStore } from '../stores/ui'
import { useWishlistStore } from '../stores/wishlist'

const route = useRoute()
const router = useRouter()
const plans = usePlansStore()
const editor = useEditorStore()
const wishlist = useWishlistStore()
const ui = useUiStore()
const { planOverlayItems, planOverlayColor } = usePlanMapOverlay()
const plan = ref(null)
const loading = ref(true)
const selectedDay = ref(1)
const selected = ref(null)
const detailOpen = ref(false)
const formOpen = ref(false)
const mapCenter = ref({ ...SEOUL_CENTER })
const days = computed(() => (plan.value ? daysBetween(plan.value.beginDate, plan.value.endDate) : []))
const dayDetails = computed(() =>
  (plan.value?.details || [])
    .filter((detail) => Number(detail.dayNo) === Number(selectedDay.value))
    .sort((a, b) => String(a.beginTime).localeCompare(String(b.beginTime))),
)
const markers = computed(() => dayDetails.value.map((detail) => detail.item).filter(Boolean))
const selectedMapId = computed(() => (selected.value ? `${selected.value.kind}:${selected.value.sceneId ?? selected.value.placeId}` : ''))
const routeMarkers = computed(() => (planOverlayItems.value.length ? planOverlayItems.value : markers.value))

const load = async () => {
  loading.value = true
  try { plan.value = await plans.getById(route.params.planId, true) }
  catch (error) { ui.toast(error.message, 'error') }
  finally { loading.value = false }
}
const openItem = async (item) => {
  selected.value = item
  if (item?.latitude != null && item?.longitude != null) mapCenter.value = { latitude: item.latitude, longitude: item.longitude }
  detailOpen.value = true
  await nextTick()
  if (item?.latitude != null && item?.longitude != null) mapCenter.value = { latitude: item.latitude, longitude: item.longitude }
}
const edit = async () => {
  try { await editor.open(plan.value); ui.toast('일정 편집기를 열었습니다.', 'success') }
  catch (error) { ui.toast(error.message, 'error') }
}
const updateInfo = async (form) => {
  try { plan.value = await plans.updateInfo(plan.value.planId, form); formOpen.value = false; ui.toast('여행 정보를 수정했습니다.', 'success') }
  catch (error) { ui.toast(error.message, 'error') }
}
const remove = async () => {
  if (!window.confirm(`‘${plan.value.title}’ 여행 계획을 삭제할까요?`)) return
  try { await plans.remove(plan.value.planId); if (String(editor.activePlan?.planId) === String(plan.value.planId)) editor.close(); router.push('/plans') }
  catch (error) { ui.toast(error.message, 'error') }
}
const toggle = async (scene) => {
  try { await wishlist.toggle(scene); ui.toast(wishlist.has(scene.sceneId) ? '위시리스트에 담았습니다.' : '위시리스트에서 제거했습니다.', 'success') }
  catch (error) { ui.toast(error.message, 'error') }
}

onMounted(load)
</script>

<template>
  <LoadingState v-if="loading" />
  <EmptyState v-else-if="!plan" title="여행 계획을 찾을 수 없습니다." description="삭제되었거나 접근 권한이 없는 계획입니다."><RouterLink class="button secondary" to="/plans">목록으로</RouterLink></EmptyState>
  <div v-else class="split-page plan-detail-page" :class="{ 'detail-open': detailOpen }">
    <section class="content-panel plan-detail-panel">
      <header class="subpage-header plan-title-header">
        <button class="icon-button" aria-label="뒤로" @click="router.push('/plans')"><AppIcon name="back" /></button>
        <div><span class="eyebrow">TRAVEL ITINERARY</span><h1>{{ plan.title }}</h1><p>{{ formatDate(plan.beginDate) }} – {{ formatDate(plan.endDate) }}</p></div>
        <div class="header-actions"><button class="icon-button" title="여행 정보 수정" @click="formOpen = true"><AppIcon name="edit" /></button><button class="icon-button danger-icon" title="삭제" @click="remove"><AppIcon name="trash" /></button></div>
      </header>
      <div class="day-tabs">
        <button v-for="day in days" :key="day.dayNo" :class="{ active: selectedDay === day.dayNo }" @click="selectedDay = day.dayNo"><span>DAY {{ day.dayNo }}</span><strong>{{ formatDate(day.date) }}</strong></button>
      </div>
      <div class="section-heading"><div><span class="eyebrow">DAY {{ selectedDay }}</span><h2>오늘의 일정</h2></div><button class="button small primary" @click="edit"><AppIcon name="edit" :size="16" />일정 편집</button></div>
      <EmptyState v-if="!dayDetails.length" title="이 날짜는 아직 비어 있습니다." description="일정 편집기를 열고 촬영지나 주변 장소를 끌어놓으세요." icon="calendar"><button class="button primary" @click="edit">일정 편집 시작</button></EmptyState>
      <div v-else class="timeline">
        <button v-for="detail in dayDetails" :key="detail.clientId" class="timeline-item" @click="openItem(detail.item)">
          <span class="timeline-time">{{ detail.beginTime }}</span><i :style="{ background: plan.color }" /><span class="timeline-copy"><strong>{{ detail.item?.name }}</strong><small>{{ detail.item?.address || (detail.item?.kind === 'scene' ? '촬영지' : '주변 장소') }}</small></span><AppIcon name="chevron" :size="17" />
        </button>
      </div>
      <div v-if="plan.memo" class="memo-card"><span class="eyebrow">MEMO</span><p>{{ plan.memo }}</p></div>
    </section>
    <section class="map-panel">
      <KakaoMap
        :items="markers"
        :fit-items="markers"
        :route-items="routeMarkers"
        :overlay-items="planOverlayItems"
        :favorites="wishlist.items"
        :center="mapCenter"
        :selected-id="selectedMapId"
        :marker-color="plan.color"
        :route-color="planOverlayColor || plan.color"
        connect-items
        @select="openItem"
        @center-change="mapCenter = $event"
      />
    </section>
    <LocationDetailPanel :open="detailOpen" :item="selected" :wished="selected?.kind === 'scene' && wishlist.has(selected.sceneId)" @close="detailOpen = false" @toggle-wishlist="toggle" />
    <PlanFormModal :open="formOpen" :plan="plan" @close="formOpen = false" @submit="updateInfo" />
  </div>
</template>
