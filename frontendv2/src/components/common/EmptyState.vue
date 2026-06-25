<script setup>
import AppIcon from './AppIcon.vue'
defineProps({ title: { type: String, default: '표시할 내용이 없습니다.' }, description: { type: String, default: '' }, icon: { type: String, default: 'pin' } })
</script>

<template>
  <div class="empty-state">
    <span class="empty-icon"><AppIcon :name="icon" :size="28" /></span>
    <strong>{{ title }}</strong>
    <p v-if="description">{{ description }}</p>
    <slot />
  </div>
</template>
