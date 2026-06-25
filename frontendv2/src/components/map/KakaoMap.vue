<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { loadKakaoMaps } from '../../api/kakao'
import { SEOUL_CENTER } from '../../config/app'
import AppIcon from '../common/AppIcon.vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  favorites: { type: Array, default: () => [] },
  selectedId: [String, Number],
  center: { type: Object, default: () => SEOUL_CENTER },
  level: { type: Number, default: 7 },
  markerColor: { type: String, default: '' },
})
const emit = defineEmits(['select', 'center-change'])

const element = ref(null)
const status = ref('loading')
let map
let markerObjects = []
let observer

const idOf = (item) => `${item.kind}:${item.sceneId ?? item.placeId}`
const selectedKey = computed(() => {
  if (String(props.selectedId || '').includes(':')) return String(props.selectedId)
  const item = props.items.find((candidate) => String(candidate.sceneId ?? candidate.placeId) === String(props.selectedId))
  return item ? idOf(item) : ''
})
const mergedItems = computed(() => {
  const mapById = new Map()
  props.favorites.forEach((item) => mapById.set(idOf(item), { ...item, favorite: true }))
  props.items.forEach((item) => mapById.set(idOf(item), { ...mapById.get(idOf(item)), ...item }))
  return [...mapById.values()].filter((item) => item.latitude != null && item.longitude != null)
})

const markerSvg = (color, selected) => {
  const size = selected ? 42 : 34
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size + 10}" viewBox="0 0 42 52"><path fill="${color}" stroke="white" stroke-width="3" d="M21 1C10 1 2 9 2 20c0 14 19 31 19 31s19-17 19-31C40 9 32 1 21 1Z"/><circle cx="21" cy="20" r="7" fill="white"/></svg>`
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

const markerColor = (item, selected) => {
  if (selected) return '#1f2937'
  if (item.favorite) return '#f35f45'
  if (props.markerColor) return props.markerColor
  return item.kind === 'scene' ? '#f35f45' : '#3e88d6'
}

const clearMarkers = () => {
  markerObjects.forEach((marker) => marker.setMap(null))
  markerObjects = []
}

const draw = ({ fit = true } = {}) => {
  if (!map || !window.kakao?.maps) return
  clearMarkers()
  const bounds = new window.kakao.maps.LatLngBounds()
  mergedItems.value.forEach((item) => {
    const position = new window.kakao.maps.LatLng(item.latitude, item.longitude)
    const selected = idOf(item) === selectedKey.value
    const imageSize = selected ? new window.kakao.maps.Size(42, 52) : new window.kakao.maps.Size(34, 44)
    const image = new window.kakao.maps.MarkerImage(markerSvg(markerColor(item, selected), selected), imageSize, {
      offset: new window.kakao.maps.Point(imageSize.width / 2, imageSize.height),
    })
    const marker = new window.kakao.maps.Marker({ map, position, image, title: item.name })
    window.kakao.maps.event.addListener(marker, 'click', () => emit('select', item))
    markerObjects.push(marker)
    bounds.extend(position)
  })
  if (!fit || !mergedItems.value.length) return
  if (mergedItems.value.length === 1) {
    map.setCenter(bounds.getSouthWest())
    map.setLevel(4)
  } else map.setBounds(bounds, 52, 52, 52, 52)
}

const init = async () => {
  try {
    await loadKakaoMaps()
    await nextTick()
    map = new window.kakao.maps.Map(element.value, {
      center: new window.kakao.maps.LatLng(props.center.latitude, props.center.longitude),
      level: props.level,
    })
    window.kakao.maps.event.addListener(map, 'idle', () => {
      const center = map.getCenter()
      emit('center-change', { latitude: center.getLat(), longitude: center.getLng() })
    })
    status.value = 'ready'
    draw()
    observer = new ResizeObserver(() => {
      map.relayout()
      draw({ fit: false })
    })
    observer.observe(element.value)
  } catch (error) {
    status.value = error.message === 'KAKAO_KEY_MISSING' ? 'missing-key' : 'error'
  }
}

watch(() => [props.items, props.favorites], () => draw(), { deep: true })
watch(() => props.selectedId, () => draw({ fit: false }))
watch(
  () => props.center,
  (center) => {
    if (map && center?.latitude != null) map.panTo(new window.kakao.maps.LatLng(center.latitude, center.longitude))
  },
  { deep: true },
)

onMounted(init)
onBeforeUnmount(() => {
  clearMarkers()
  observer?.disconnect()
})
</script>

<template>
  <div class="map-shell">
    <div ref="element" class="kakao-map" />
    <div v-if="status !== 'ready'" class="map-placeholder">
      <span class="map-placeholder-icon"><AppIcon name="pin" :size="34" /></span>
      <template v-if="status === 'missing-key'">
        <strong>카카오맵 앱 키를 설정해 주세요</strong>
        <p><code>.env</code>의 <code>VITE_KAKAO_MAP_APP_KEY</code>에 JavaScript 키를 입력하면 지도가 표시됩니다.</p>
      </template>
      <template v-else-if="status === 'error'">
        <strong>지도를 불러오지 못했습니다</strong>
        <p>카카오 개발자 콘솔의 JavaScript SDK 도메인과 네트워크 상태를 확인해 주세요.</p>
      </template>
      <template v-else><span class="spinner" /> 지도를 준비하는 중입니다.</template>
    </div>
    <div v-if="status === 'ready'" class="map-legend">
      <span><i class="legend-dot scene" />촬영지·위시리스트</span>
      <span><i class="legend-dot place" />주변 장소</span>
    </div>
    <slot />
  </div>
</template>
