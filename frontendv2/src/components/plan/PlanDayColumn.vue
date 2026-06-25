<script setup>
import { computed, ref } from 'vue'
import { formatDate } from '../../models/date'
import AppIcon from '../common/AppIcon.vue'

const props = defineProps({ day: { type: Object, required: true }, details: { type: Array, default: () => [] } })
const emit = defineEmits(['external-drop', 'move', 'remove', 'time'])
const over = ref(false)
const ordered = computed(() => [...props.details].sort((a, b) => String(a.beginTime).localeCompare(String(b.beginTime))))

const read = (event, type) => {
  const raw = event.dataTransfer.getData(type)
  if (!raw) return null
  try { return type.endsWith('item') ? JSON.parse(raw) : raw } catch { return null }
}
const drop = (event, beforeClientId = null) => {
  over.value = false
  const external = read(event, 'application/x-pilgrimage-item')
  if (external) return emit('external-drop', external, props.day.dayNo)
  const clientId = read(event, 'application/x-pilgrimage-detail')
  if (clientId) emit('move', clientId, props.day.dayNo, beforeClientId)
}
const dragDetail = (event, detail) => {
  event.dataTransfer.effectAllowed = 'move'
  event.dataTransfer.setData('application/x-pilgrimage-detail', detail.clientId)
}
</script>

<template>
  <section class="plan-day" :class="{ over }" @dragenter.prevent="over = true" @dragleave.self="over = false" @dragover.prevent @drop.prevent="drop($event)">
    <header><span>DAY {{ day.dayNo }}</span><strong>{{ formatDate(day.date) }}</strong></header>
    <div class="plan-day-list">
      <article
        v-for="detail in ordered"
        :key="detail.clientId"
        class="plan-detail-row"
        draggable="true"
        @dragstart="dragDetail($event, detail)"
        @dragover.prevent
        @drop.stop.prevent="drop($event, detail.clientId)"
      >
        <span class="drag-handle"><AppIcon name="drag" :size="17" /></span>
        <div><strong>{{ detail.item?.name || '장소 정보 불러오는 중' }}</strong><small>{{ detail.item?.kind === 'scene' ? '촬영지' : '주변 장소' }}</small></div>
        <label class="time-input"><AppIcon name="clock" :size="14" /><input :value="detail.beginTime" type="time" @input="emit('time', detail.clientId, $event.target.value)" /></label>
        <button class="icon-button tiny" aria-label="일정에서 제거" @click="emit('remove', detail.clientId)"><AppIcon name="close" :size="15" /></button>
      </article>
      <div v-if="!ordered.length" class="day-empty"><AppIcon name="plus" :size="20" /><span>촬영지나 장소를<br />여기로 끌어놓으세요.</span></div>
    </div>
  </section>
</template>
