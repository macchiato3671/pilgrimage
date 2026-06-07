<template>
  <main>
    <MapComponent 
      :width="componentWidth"
      :height="componentHeight"
      @ready="handleMapReady"
    />
  </main>
</template>

<script setup>
import MapComponent from '@/components/common/MapComponent.vue'
import { ref } from 'vue'

const componentWidth = ref('50%')
const componentHeight = ref('400px')

const kakaoSdk = ref(null)
const kakaoMap = ref(null)

const handleMapReady = ({ kakao, map }) => {
  kakaoSdk.value = kakao
  kakaoMap.value = map
}
</script>
