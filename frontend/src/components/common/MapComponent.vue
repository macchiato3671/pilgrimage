<template>
  <div ref="mapContainer" class="map"></div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

// PROPS
const props = defineProps({
  width: {
    type: String,
    default: '100%',
  },
  height: {
    type: String,
    default: '100%',
  },
})

// EMITS
const emit = defineEmits([
  'ready',
])

// DATA
const APP_KEY = import.meta.env.VITE_KAKAO_MAP_KEY
const mapContainer = ref(null)
const map = ref(null)
const mapSdk = ref(null)

let resizeObserver = null
let resizeAnimationFrameId = null

// METHODS
const loadKakaoMapScript = () => {
  return new Promise((resolve, reject) => {
    if (window?.kakao?.maps) {
      resolve(window.kakao)
      return
    }

    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${APP_KEY}&autoload=false`
    script.onerror = reject
    script.onload = () => {
      window.kakao.maps.load(() => {
        resolve(window.kakao)
      })
    }

    document.head.appendChild(script)
  })
}
const setupResizeObserver = () => {
  if (!mapContainer.value) return

  resizeObserver = new ResizeObserver(() => {
    if (!map.value) return;

    if (resizeAnimationFrameId)
      cancelAnimationFrame(resizeAnimationFrameId)

    resizeAnimationFrameId = requestAnimationFrame(() => {
      const { width, height } = mapContainer.value.getBoundingClientRect()
      if (width === 0 || height === 0) return
      const center = map.value.getCenter()
      map.value.relayout()
      map.value.setCenter(center)
    })
  })

  resizeObserver.observe(mapContainer.value)
}

const createMap = async () => {
  mapSdk.value = await loadKakaoMapScript()
  
  const options = {
    center: new mapSdk.value.maps.LatLng(37.5665, 126.9780),
    level: 3,
  }

  map.value = new mapSdk.value.maps.Map(mapContainer.value, options)

  setupResizeObserver()

  emit('ready', {
    _mapSdk: mapSdk.value,
    _map: map.value,
  })
}

// ON MOUNT
onMounted(() => {
  createMap()
})
onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  }

  if (resizeAnimationFrameId) {
    cancelAnimationFrame(resizeAnimationFrameId)
  }
})
</script>

<style lang="scss" scoped>
  .map {
    width:  v-bind('props.width');
    height: v-bind('props.height');
  }
</style>
