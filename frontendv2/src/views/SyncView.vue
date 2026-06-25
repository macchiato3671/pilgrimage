<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import brandMark from '../assets/images/brand-mark.png'
import AppIcon from '../components/common/AppIcon.vue'
import { useSyncStore } from '../stores/sync'

const sync = useSyncStore()
const route = useRoute()
const router = useRouter()
const done = ref(false)
const wishlistCount = computed(() => sync.snapshot.wishlist.length)
const planCount = computed(() => sync.snapshot.plans.length)
const next = () => router.replace(sync.resolveNextPath(String(route.query.next || '/plans')))

const run = async () => {
  try { await sync.run(); done.value = true } catch { /* 로그에 표시 */ }
}
const discard = () => {
  if (!window.confirm('이 브라우저에 저장된 비회원 작업을 모두 삭제할까요?')) return
  sync.discard()
  done.value = true
}
onMounted(() => {
  sync.refresh()
  if (!wishlistCount.value && !planCount.value) next()
})
</script>

<template>
  <main class="sync-page">
    <section class="sync-card">
      <img class="sync-logo" :src="brandMark" alt="" />
      <template v-if="!done">
        <span class="eyebrow">LOCAL WORKSPACE FOUND</span>
        <h1>비회원 작업을 계정에<br />가져올까요?</h1>
        <p>로그인 전에 만든 작업이 이 브라우저에 남아 있습니다. 서버에 등록하면 이후 계정에서 계속 편집할 수 있으며, 완료된 로컬 데이터는 삭제됩니다.</p>
        <div class="sync-summary">
          <div><AppIcon name="heart" :size="24" /><span><strong>{{ wishlistCount }}</strong><small>위시리스트 촬영지</small></span></div>
          <div><AppIcon name="calendar" :size="24" /><span><strong>{{ planCount }}</strong><small>여행 계획</small></span></div>
        </div>
        <div v-if="sync.running || sync.logs.length" class="sync-progress-wrap">
          <div class="progress-label"><span>{{ sync.running ? '계정에 등록하는 중…' : '동기화 기록' }}</span><strong>{{ sync.progress }}%</strong></div>
          <div class="progress"><i :style="{ width: `${sync.progress}%` }" /></div>
          <ul class="sync-log"><li v-for="(log, index) in sync.logs" :key="index" :class="log.type"><AppIcon :name="log.type === 'error' ? 'alert' : 'check'" :size="15" />{{ log.message }}</li></ul>
        </div>
        <div class="sync-actions"><button class="button primary" :disabled="sync.running" @click="run"><AppIcon name="sync" :size="17" />{{ sync.running ? '동기화 중…' : '계정에 등록하기' }}</button><button class="button secondary" :disabled="sync.running" @click="discard">로컬 작업 삭제하고 계속</button></div>
      </template>
      <template v-else>
        <span class="sync-complete"><AppIcon name="check" :size="36" /></span>
        <h1>준비가 완료되었습니다.</h1>
        <p>{{ sync.logs.at(-1)?.message || '계정으로 이동해 여행을 계속하세요.' }}</p>
        <button class="button primary" @click="next">필그리미지 계속하기</button>
      </template>
    </section>
  </main>
</template>
