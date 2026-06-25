<script setup>
import { computed } from 'vue'
import { fallbackFor } from '../../assets/fallbacks'
import AppIcon from './AppIcon.vue'
import BaseModal from './BaseModal.vue'
import ImageGallery from './ImageGallery.vue'

const props = defineProps({ open: Boolean, item: Object, wished: Boolean })
const emit = defineEmits(['close', 'toggle-wishlist', 'nearby'])
const coordinate = computed(() => {
  if (props.item?.latitude == null || props.item?.longitude == null) return '좌표 정보 없음'
  return `${props.item.latitude}, ${props.item.longitude}`
})
const mapLink = computed(() => {
  const item = props.item
  if (!item || item.latitude == null || item.longitude == null) return ''
  return `https://map.kakao.com/link/map/${encodeURIComponent(item.name)},${item.latitude},${item.longitude}`
})
</script>

<template>
  <BaseModal :open="open" :title="item?.name || '상세 정보'" width="760px" @close="emit('close')">
    <template v-if="item">
      <ImageGallery :images="item.images" :fallback="fallbackFor(item)" :alt="item.name" />
      <div class="detail-copy">
        <span class="detail-badge">{{ item.kind === 'scene' ? item.dramaTitle || '촬영지' : item.contentTypeName || '주변 장소' }}</span>
        <p>{{ item.description || '등록된 설명이 없습니다.' }}</p>
        <dl class="detail-list">
          <div><dt><AppIcon name="pin" :size="17" />주소</dt><dd>{{ item.address || '주소 정보 없음' }}</dd></div>
          <div><dt><AppIcon name="search" :size="17" />찾아가는 팁</dt><dd>{{ item.tips || '등록된 상세 안내가 없습니다.' }}</dd></div>
          <div><dt><AppIcon name="pin" :size="17" />좌표</dt><dd>{{ coordinate }}</dd></div>
        </dl>
      </div>
      <div class="detail-actions">
        <a v-if="mapLink" class="button secondary" :href="mapLink" target="_blank" rel="noreferrer">
          카카오맵에서 보기 <AppIcon name="external" :size="16" />
        </a>
        <button v-if="item.kind === 'scene'" class="button secondary" @click="emit('nearby', item)">주변 장소 찾기</button>
        <button
          v-if="item.kind === 'scene'"
          class="button"
          :class="wished ? 'danger' : 'primary'"
          @click="emit('toggle-wishlist', item)"
        >
          <AppIcon name="heart" :size="17" /> {{ wished ? '위시리스트에서 제거' : '위시리스트에 담기' }}
        </button>
      </div>
    </template>
  </BaseModal>
</template>
