<script setup>
import { storeToRefs } from 'pinia'
import { useUiStore } from '../../stores/ui'
import AppIcon from './AppIcon.vue'
const ui = useUiStore()
const { toasts } = storeToRefs(ui)
</script>

<template>
  <Teleport to="body">
    <div class="toast-stack" aria-live="polite">
      <TransitionGroup name="toast">
        <button v-for="toast in toasts" :key="toast.id" class="toast" :class="`toast-${toast.type}`" @click="ui.dismiss(toast.id)">
          <AppIcon :name="toast.type === 'error' ? 'alert' : 'check'" :size="18" />
          <span>{{ toast.message }}</span>
        </button>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
