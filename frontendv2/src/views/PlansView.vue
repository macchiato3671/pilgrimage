<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppIcon from '../components/common/AppIcon.vue'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import KakaoMap from '../components/map/KakaoMap.vue'
import PlanFormModal from '../components/plan/PlanFormModal.vue'
import { dateRange } from '../models/date'
import { useAuthStore } from '../stores/auth'
import { useEditorStore } from '../stores/editor'
import { usePlansStore } from '../stores/plans'
import { useUiStore } from '../stores/ui'
import { useWishlistStore } from '../stores/wishlist'

const plans = usePlansStore()
const editor = useEditorStore()
const auth = useAuthStore()
const wishlist = useWishlistStore()
const ui = useUiStore()
const router = useRouter()
const formOpen = ref(false)
const activeMarkers = computed(() =>
  [...(editor.activePlan?.details || [])]
    .sort((a, b) => Number(a.dayNo) - Number(b.dayNo) || String(a.beginTime).localeCompare(String(b.beginTime)))
    .map((detail) => detail.item)
    .filter(Boolean),
)

const newPlan = async () => {
  if (editor.isOpen && editor.dirty) {
    if (!window.confirm('현재 편집 내용을 저장하고 새 여행 계획을 만들까요?')) return
    try { await editor.save() } catch (error) { return ui.toast(error.message, 'error') }
  }
  formOpen.value = true
}
const create = async (form) => {
  try {
    const plan = await plans.create(form)
    formOpen.value = false
    await editor.open(plan)
    ui.toast('새 여행 계획을 만들었습니다. 장소를 끌어놓아 일정을 구성하세요.', 'success')
  } catch (error) { ui.toast(error.message, 'error') }
}
const edit = async (plan) => {
  if (editor.dirty && String(editor.activePlan?.planId) !== String(plan.planId) && !window.confirm('현재 편집 중인 변경사항을 버리고 다른 계획을 열까요?')) return
  try { await editor.open(plan) } catch (error) { ui.toast(error.message, 'error') }
}
const remove = async (plan) => {
  if (!window.confirm(`‘${plan.title}’ 여행 계획을 삭제할까요?`)) return
  try {
    await plans.remove(plan.planId)
    if (String(editor.activePlan?.planId) === String(plan.planId)) editor.close()
    ui.toast('여행 계획을 삭제했습니다.', 'success')
  } catch (error) { ui.toast(error.message, 'error') }
}
</script>

<template>
  <div class="split-page plans-page">
    <section class="content-panel plans-panel">
      <header class="page-heading compact-heading"><span class="eyebrow">TRAVEL PLANS</span><h1>여행 계획</h1><p>{{ auth.isAuthenticated ? '계정에 저장된 여행 일정을 관리하세요.' : '비회원 계획은 이 브라우저에 저장되며 로그인 후 동기화할 수 있습니다.' }}</p></header>
      <button class="new-plan-button" @click="newPlan"><span><AppIcon name="plus" :size="22" /></span><strong>새 여행 계획</strong><small>날짜와 핀 색상을 선택해 시작하기</small></button>
      <LoadingState v-if="plans.loading" />
      <EmptyState v-else-if="!plans.items.length" title="아직 만든 여행 계획이 없습니다." description="새 여행 계획을 만들고 촬영지와 주변 장소를 끌어놓으세요." icon="calendar" />
      <div v-else class="plan-list">
        <article v-for="plan in plans.items" :key="plan.planId" class="plan-card" :class="{ editing: String(editor.activePlan?.planId) === String(plan.planId) }">
          <button class="plan-card-main" @click="router.push({ name: 'plan-detail', params: { planId: plan.planId } })">
            <i :style="{ background: plan.color }" /><span><strong>{{ plan.title }}</strong><small>{{ dateRange(plan.beginDate, plan.endDate) }}</small><em v-if="String(editor.activePlan?.planId) === String(plan.planId)">현재 편집 중</em></span><AppIcon name="chevron" :size="18" />
          </button>
          <div class="plan-card-actions"><button title="일정 편집" @click="edit(plan)"><AppIcon name="edit" :size="17" /></button><button title="삭제" @click="remove(plan)"><AppIcon name="trash" :size="17" /></button></div>
        </article>
      </div>
    </section>
    <section class="map-panel">
      <KakaoMap
        :items="activeMarkers"
        :fit-items="activeMarkers"
        :route-items="activeMarkers"
        :favorites="wishlist.items"
        :marker-color="editor.activePlan?.color"
        :route-color="editor.activePlan?.color"
        connect-items
      />
    </section>
    <PlanFormModal :open="formOpen" @close="formOpen = false" @submit="create" />
  </div>
</template>
