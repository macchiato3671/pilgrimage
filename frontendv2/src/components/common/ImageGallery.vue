<script setup>
import { computed, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({ images: { type: Array, default: () => [] }, fallback: String, alt: String })
const index = ref(0)
watch(() => props.images, () => (index.value = 0))
const all = computed(() => (props.images?.length ? props.images : props.fallback ? [props.fallback] : []))
const active = computed(() => all.value[index.value])
</script>

<template>
  <div class="gallery">
    <div class="gallery-main">
      <img v-if="active" :src="active" :alt="alt" />
      <span v-else class="image-placeholder"><AppIcon name="image" :size="34" /></span>
    </div>
    <div v-if="all.length > 1" class="gallery-thumbs">
      <button v-for="(image, imageIndex) in all" :key="image" :class="{ active: index === imageIndex }" @click="index = imageIndex">
        <img :src="image" :alt="`${alt} 이미지 ${imageIndex + 1}`" />
      </button>
    </div>
  </div>
</template>
