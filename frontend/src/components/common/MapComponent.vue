<template>
  <div ref="mapContainer" class="map"></div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

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

const createMap = async () => {
  const kakao = await loadKakaoMapScript()
  
  const options = {
    center: new kakao.maps.LatLng(37.5665, 126.9780),
    level: 3,
  }

  const map = new kakao.maps.Map(mapContainer.value, options)

  emit('ready', {
    kakao,
    map,
  })
}

// ON MOUNT
onMounted(() => {
  createMap()
})
</script>

<style lang="scss" scoped>
  .map {
    width:  v-bind('props.width');
    height: v-bind('props.height');
  }
</style>
