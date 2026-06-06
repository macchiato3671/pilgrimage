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

// DATA
const APP_KEY = import.meta.env.VITE_KAKAO_MAP_KEY
const mapContainer = ref(null)
const map = ref(null)

// METHODS
const loadKakaoMapScript = () => {
  return new Promise((resolve, reject) => {
    // 1. Check whether Kakao map script is already loaded or not.
    if (window?.kakao?.maps) {
      // 1.1. If Kakao map script is already loaded, then resolve the promise with Kakao map Javascript object.
      resolve(window.kakao)
      return
    }

    // 2. Upon case no Kakao map script is loaded, create a (Java)script element on the document.
    const script = document.createElement('script')
    script.type = 'text/javascript'

    // 2.1. Set script element's source as Kakao map Javascript SDK
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${APP_KEY}&autoload=false`

    // 2.2. Set script's onload behavior
    // 2.2.1. Wait until the script is fully loaded. (Kakao map script is fully downloaded and loaded)
    script.onload = () => {
      // 2.2.2. Then wait until the kakao.map SDK is fully loaded.
      window.kakao.maps.load(() => {
        // 2.2.3. When everything is ready, then resolve the promise with Kakao map Javascript object.
        resolve(window.kakao)
      })
    }

    // 2.3. Set script element's onerror behavior (as reject the promise with an error)
    script.onerror = reject

    // 3. Actual attachment of the script element to the document
    document.head.appendChild(script)
  })
}

const createMap = async () => {
  const kakao = await loadKakaoMapScript()
  
  const options = {
    center: new kakao.maps.LatLng(37.5665, 126.9780),
    level: 3,
  }

  map.value = new kakao.maps.Map(mapContainer.value, options)
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