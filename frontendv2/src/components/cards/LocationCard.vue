<script setup>
import { computed } from 'vue'
import { fallbackFor } from '../../assets/fallbacks'
import { useEditorStore } from '../../stores/editor'
import AppIcon from '../common/AppIcon.vue'

const props = defineProps({ item: { type: Object, required: true }, wished: Boolean, compact: Boolean })
const emit = defineEmits(['select', 'toggle-wishlist'])
const editor = useEditorStore()
const image = computed(() => props.item.images?.[0] || fallbackFor(props.item))
const label = computed(() => (props.item.kind === 'scene' ? props.item.dramaTitle || '드라마 촬영지' : props.item.contentTypeName || '주변 장소'))

const dragstart = (event) => {
  event.dataTransfer.effectAllowed = 'copy'
  event.dataTransfer.setData('application/x-pilgrimage-item', JSON.stringify(props.item))
  editor.beginExternalDrag()
}
</script>

<template>
  <article
    class="location-card"
    :class="{ compact, draggable: editor.isOpen }"
    :draggable="editor.isOpen"
    @dragstart="dragstart"
    @dragend="editor.endDrag"
    @click="emit('select', item)"
  >
    <img :src="image" :alt="item.name" />
    <div class="location-card-body">
      <span class="eyebrow">{{ label }}</span>
      <strong>{{ item.name }}</strong>
      <p class="line-clamp-2">{{ item.description || '상세 정보를 확인해 보세요.' }}</p>
      <span class="address"><AppIcon name="pin" :size="14" />{{ item.address || '주소 정보 없음' }}</span>
    </div>
    <button
      v-if="item.kind === 'scene'"
      class="heart-button"
      :class="{ active: wished }"
      :aria-label="wished ? '위시리스트에서 제거' : '위시리스트에 추가'"
      @click.stop="emit('toggle-wishlist', item)"
    >
      <AppIcon name="heart" :size="19" />
    </button>
    <span v-if="editor.isOpen" class="drag-hint"><AppIcon name="drag" :size="16" /> 일정에 끌어놓기</span>
  </article>
</template>
