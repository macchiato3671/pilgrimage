<script setup>
import { computed } from 'vue'
import { fallbackFor } from '../../assets/fallbacks'
import AppIcon from '../common/AppIcon.vue'

const props = defineProps({ drama: { type: Object, required: true }, index: { type: Number, default: 0 } })
const emit = defineEmits(['select'])
const image = computed(() => props.drama.images?.[0] || fallbackFor(props.drama, props.index))
</script>

<template>
  <button class="drama-card" @click="emit('select', drama)">
    <img :src="image" :alt="`${drama.title} 포스터`" />
    <span class="drama-card-body">
      <span class="eyebrow">{{ drama.year || '연도 미상' }}<template v-if="drama.genres.length"> · {{ drama.genres.slice(0, 2).join(', ') }}</template></span>
      <strong>{{ drama.title }}</strong>
      <span class="line-clamp-2">{{ drama.description || '작품과 연결된 촬영지를 확인해 보세요.' }}</span>
      <span class="card-meta"><AppIcon name="pin" :size="15" /> {{ drama.sceneCount }}개 촬영지</span>
    </span>
    <AppIcon class="card-chevron" name="chevron" :size="18" />
  </button>
</template>
