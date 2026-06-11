<template>
  <section class="schedule-section">
    <header class="schedule-header">
      <button type="button" @click="$emit('cancel')">
        취소
      </button>

      <h2>여행 일정</h2>

      <button type="button">
        저장
      </button>
    </header>

    <section class="schedule-body">
      <p>{{ draftTitle }}</p>
      <p>{{ draftDateText }}</p>

      <div class="day-tabs">
        <button
          v-for="day in days"
          :key="day.dayNo"
          type="button"
          :class="{ active: activeDayNo === day.dayNo }"
          @click="$emit('change-day', day.dayNo)"
        >
          {{ day.dayNo }}일차
        </button>
      </div>

      <section
        class="schedule-drop-area"
        @dragover.prevent
        @drop.prevent="$emit('drop-to-schedule', $event)"
      >
        <p v-if="currentDayDetails.length === 0" class="empty-message">
          오른쪽 위시리스트에서 목적지를 드래그해서 일정을 추가하세요.
        </p>

        <ScheduleDetailCard
          v-for="detail in currentDayDetails"
          :key="detail.tempId"
          :detail="detail"
          @detail-drag-start="$emit('detail-drag-start', detail, $event)"
          @drag-end="$emit('drag-end')"
          @click-detail="$emit('click-detail', detail)"
          @remove-detail="$emit('remove-detail', detail.tempId)"
          @update-begin-time="$emit('update-begin-time', $event)"
        />
      </section>
    </section>
  </section>
</template>

<script setup>
import ScheduleDetailCard from './ScheduleDetailCard.vue';

defineProps({
  draftTitle: {
    type: String,
    required: true,
  },
  draftDateText: {
    type: String,
    required: true,
  },
  days: {
    type: Array,
    required: true,
  },
  activeDayNo: {
    type: Number,
    required: true,
  },
  currentDayDetails: {
    type: Array,
    required: true,
  },
});

defineEmits([
  'cancel',
  'change-day',
  'drop-to-schedule',
  'detail-drag-start',
  'drag-end',
  'click-detail',
  'remove-detail',
  'update-begin-time',
]);
</script>

<style scoped>
.schedule-section {
  border-left: 1px solid #ddd;
  border-right: 1px solid #ddd;
  padding: 16px;
  overflow-y: auto;
}

.schedule-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.schedule-body {
  margin-top: 24px;
}

.day-tabs {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  margin-bottom: 16px;
}

.day-tabs button.active {
  font-weight: 700;
}

.schedule-drop-area {
  min-height: 280px;
  padding: 16px;
  border: 1px dashed #bbb;
  border-radius: 8px;
}

.empty-message {
  margin-top: 32px;
  color: #777;
}
</style>