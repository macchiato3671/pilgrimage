<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import DramaCard from '../components/cards/DramaCard.vue'
import EmptyState from '../components/common/EmptyState.vue'
import LoadingState from '../components/common/LoadingState.vue'
import SearchBox from '../components/common/SearchBox.vue'
import KakaoMap from '../components/map/KakaoMap.vue'
import { useExploreStore } from '../stores/explore'
import { useUiStore } from '../stores/ui'
import { useWishlistStore } from '../stores/wishlist'

const explore = useExploreStore()
const wishlist = useWishlistStore()
const ui = useUiStore()
const router = useRouter()
const query = ref(explore.keyword)
const error = ref('')
const heading = computed(() => {
  if (explore.keyword) return `‘${explore.keyword}’ 검색 결과`
  if (explore.mode === 'genre') return explore.genres.find((genre) => String(genre.genreId) === String(explore.selectedGenre))?.name || '장르별 작품'
  return `${explore.selectedYear || ''}년 작품`
})

const run = async (action) => {
  error.value = ''
  try { await action() } catch (reason) { error.value = reason.message; ui.toast(reason.message, 'error') }
}
const openDrama = (drama) => router.push({ name: 'drama-scenes', params: { dramaId: drama.dramaId } })

onMounted(() => {
  if (!explore.years.length) run(() => explore.bootstrap())
  else if (!explore.dramas.length) run(() => explore.loadPage())
})
</script>

<template>
  <div class="split-page">
    <section class="content-panel catalog-panel">
      <header class="page-heading">
        <span class="eyebrow">K-DRAMA PILGRIMAGE</span>
        <h1>작품 속 장면을<br />여행지로 만나보세요.</h1>
        <p>좋아한 작품을 고르면 서울의 촬영지와 주변 여행 장소를 한 번에 연결해 드립니다.</p>
      </header>
      <SearchBox v-model="query" placeholder="드라마·영화 제목 검색" :busy="explore.loading" @search="run(() => explore.search($event))" />
      <div class="filter-row">
        <div class="segmented">
          <button :class="{ active: explore.mode === 'year' && !explore.keyword }" @click="run(() => explore.loadByYear(explore.selectedYear || explore.years[0]))">연도별</button>
          <button :class="{ active: explore.mode === 'genre' && !explore.keyword }" @click="run(() => explore.loadByGenre(explore.selectedGenre || explore.genres[0]?.genreId))">장르별</button>
        </div>
        <select v-if="explore.mode === 'year'" :value="explore.selectedYear" @change="run(() => explore.loadByYear($event.target.value))">
          <option v-for="year in explore.years" :key="year" :value="year">{{ year }}년</option>
        </select>
        <select v-else :value="explore.selectedGenre" @change="run(() => explore.loadByGenre($event.target.value))">
          <option v-for="genre in explore.genres" :key="genre.genreId" :value="genre.genreId">{{ genre.name }}</option>
        </select>
      </div>
      <div class="section-heading"><div><span class="eyebrow">DISCOVER</span><h2>{{ heading }}</h2></div><span>{{ explore.dramas.length }}개</span></div>
      <LoadingState v-if="explore.loading && !explore.dramas.length" />
      <EmptyState v-else-if="error" title="작품을 불러오지 못했습니다." :description="error"><button class="button secondary" @click="run(() => explore.bootstrap())">다시 시도</button></EmptyState>
      <EmptyState v-else-if="!explore.dramas.length" title="조건에 맞는 작품이 없습니다." description="다른 연도·장르 또는 검색어를 선택해 보세요." icon="film" />
      <div v-else class="card-list">
        <DramaCard v-for="(drama, index) in explore.dramas" :key="drama.dramaId" :drama="drama" :index="index" @select="openDrama" />
        <button v-if="explore.hasNext" class="button secondary load-more" :disabled="explore.loading" @click="run(() => explore.loadMore())">{{ explore.loading ? '불러오는 중…' : '작품 더 보기' }}</button>
      </div>
    </section>
    <section class="map-panel">
      <KakaoMap :favorites="wishlist.items" />
      <div class="map-callout"><strong>위시리스트 촬영지는 항상 지도에 표시됩니다.</strong><span>마커를 눌러 저장한 장소를 다시 확인하세요.</span></div>
    </section>
  </div>
</template>
