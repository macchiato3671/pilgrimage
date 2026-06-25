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
  connectItems: Boolean,
  mapPadding: { type: Object, default: () => ({ top: 52, right: 52, bottom: 52, left: 52 }) },
})
const emit = defineEmits(['select', 'center-change'])

const element = ref(null)
const status = ref('loading')
let map
let markerObjects = []
let polylineObject = null
let observer
let movingFromProp = false

const idOf = (item) => `${item.kind}:${item.sceneId ?? item.placeId}`
const hasCoordinate = (item) => item?.latitude != null && item?.longitude != null
const latLngOf = (item) => new window.kakao.maps.LatLng(item.latitude, item.longitude)
const closeEnough = (a, b) => {
  if (!a || !b) return false
  return Math.abs(a.latitude - b.latitude) < 0.00001 && Math.abs(a.longitude - b.longitude) < 0.00001
}
const selectedKey = computed(() => {
  if (String(props.selectedId || '').includes(':')) return String(props.selectedId)
  const item = props.items.find((candidate) => String(candidate.sceneId ?? candidate.placeId) === String(props.selectedId))
  return item ? idOf(item) : ''
})
const mergedItems = computed(() => {
  const mapById = new Map()
  props.favorites.forEach((item) => mapById.set(idOf(item), { ...item, favorite: true, primary: false }))
  props.items.forEach((item) => mapById.set(idOf(item), { ...mapById.get(idOf(item)), ...item, primary: true }))
  return [...mapById.values()].filter(hasCoordinate)
})
const selectedItem = computed(() => mergedItems.value.find((item) => idOf(item) === selectedKey.value))
const routeItems = computed(() => props.items.filter(hasCoordinate))
const padding = computed(() => ({
  top: props.mapPadding?.top ?? 52,
  right: props.mapPadding?.right ?? 52,
  bottom: props.mapPadding?.bottom ?? 52,
  left: props.mapPadding?.left ?? 52,
}))

const markerSvg = (color, selected) => {
  const size = selected ? 42 : 34
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size + 10}" viewBox="0 0 42 52"><path fill="${color}" stroke="white" stroke-width="3" d="M21 1C10 1 2 9 2 20c0 14 19 31 19 31s19-17 19-31C40 9 32 1 21 1Z"/><circle cx="21" cy="20" r="7" fill="white"/></svg>`
  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

const markerColor = (item, selected) => {
  if (selected) return '#1f2937'
  if (item.primary && props.markerColor) return props.markerColor
  if (item.favorite) return '#f35f45'
  if (props.markerColor) return props.markerColor
  return item.kind === 'scene' ? '#f35f45' : '#3e88d6'
}

const clearMarkers = () => {
  markerObjects.forEach((marker) => marker.setMap(null))
  markerObjects = []
}
const clearPolyline = () => {
  polylineObject?.setMap(null)
  polylineObject = null
}

const drawPolyline = () => {
  clearPolyline()
  if (!props.connectItems || routeItems.value.length < 2) return
  polylineObject = new window.kakao.maps.Polyline({
    map,
    path: routeItems.value.map(latLngOf),
    strokeWeight: 4,
    strokeColor: props.markerColor || '#3e88d6',
    strokeOpacity: 0.78,
    strokeStyle: 'solid',
  })
}

const focusItem = (item, { level } = {}) => {
  if (!map || !window.kakao?.maps || !hasCoordinate(item)) return
  const current = map.getCenter()
  const next = { latitude: item.latitude, longitude: item.longitude }
  if (closeEnough({ latitude: current.getLat(), longitude: current.getLng() }, next)) return
  movingFromProp = true
  if (level) map.setLevel(level)
  map.panTo(latLngOf(item))
}

const draw = ({ fit = true } = {}) => {
  if (!map || !window.kakao?.maps) return
  clearMarkers()
  drawPolyline()
  const bounds = new window.kakao.maps.LatLngBounds()
  mergedItems.value.forEach((item) => {
    const position = latLngOf(item)
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
    focusItem(mergedItems.value[0], { level: 4 })
  } else map.setBounds(bounds, padding.value.top, padding.value.right, padding.value.bottom, padding.value.left)
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
      const next = { latitude: center.getLat(), longitude: center.getLng() }
      if (movingFromProp && closeEnough(props.center, next)) {
        movingFromProp = false
        return
      }
      movingFromProp = false
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

watch(() => [props.items, props.favorites, props.connectItems, props.markerColor, props.mapPadding], () => draw(), { deep: true })
watch(() => props.selectedId, () => { draw({ fit: false }); focusItem(selectedItem.value) })
watch(
  () => props.center,
  (center) => {
    if (!map || center?.latitude == null) return
    const current = map.getCenter()
    if (closeEnough({ latitude: current.getLat(), longitude: current.getLng() }, center)) return
    movingFromProp = true
    map.panTo(new window.kakao.maps.LatLng(center.latitude, center.longitude))
  },
  { deep: true },
)

onMounted(init)
onBeforeUnmount(() => {
  clearMarkers()
  clearPolyline()
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
