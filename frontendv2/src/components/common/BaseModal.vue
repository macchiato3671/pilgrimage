<script setup>
import AppIcon from './AppIcon.vue'
defineProps({ open: Boolean, title: { type: String, default: '' }, width: { type: String, default: '640px' } })
const emit = defineEmits(['close'])
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="open" class="modal-backdrop" @mousedown.self="emit('close')">
        <section class="modal-card" :style="{ width }" role="dialog" aria-modal="true" :aria-label="title">
          <header v-if="title || $slots.header" class="modal-header">
            <slot name="header"><h2>{{ title }}</h2></slot>
            <button class="icon-button" aria-label="닫기" @click="emit('close')"><AppIcon name="close" /></button>
          </header>
          <div class="modal-body"><slot /></div>
          <footer v-if="$slots.footer" class="modal-footer"><slot name="footer" /></footer>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

