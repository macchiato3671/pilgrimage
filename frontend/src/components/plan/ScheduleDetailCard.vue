<template>
  <article
    class="schedule-card"
    :data-temp-id="detail.tempId"
    draggable="true"
    @dragstart.stop="$emit('detail-drag-start', $event)"
    @dragend="$emit('drag-end')"
    @click="$emit('click-detail')"
  >
    <h3>{{ detail.name }}</h3>
    <p>{{ detail.address }}</p>

    <div class="time-row">
      <label>
        시작
        <input
          :value="detail.beginTime"
          type="time"
          @input="handleInputBeginTime"
        />
      </label>
    </div>

    <button type="button" @click.stop="$emit('remove-detail')">
      삭제
    </button>
  </article>
</template>

<script setup>
const props = defineProps({
  detail: {
    type: Object,
    required: true,
  },
});

const emit = defineEmits([
  'detail-drag-start',
  'drag-end',
  'click-detail',
  'remove-detail',
  'update-begin-time',
]);

const handleInputBeginTime = (event) => {
  emit('update-begin-time', {
    tempId: props.detail.tempId,
    beginTime: event.target.value,
  });
};
</script>

<style scoped>
.schedule-card {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin-bottom: 12px;
  background-color: #fff;
}

.time-row {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  margin-bottom: 8px;
}
</style>