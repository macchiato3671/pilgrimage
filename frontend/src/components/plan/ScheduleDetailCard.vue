<template>
  <article
    class="schedule-card"
    :data-temp-id="detail.tempId"
    :draggable="!readonly"
    @dragstart.stop="handleDragStart"
    @dragend="handleDragEnd"
    @click="$emit('click-detail')"
  >
    <h3>{{ detail.name }}</h3>
    <p>{{ detail.address }}</p>

    <div class="time-row">
      <span v-if="readonly">
        시작 {{ detail.beginTime }}
      </span>

      <label v-else>
        시작
        <input
          :value="detail.beginTime"
          type="time"
          @input="handleInputBeginTime"
        />
      </label>
    </div>

    <button
      v-if="!readonly"
      type="button"
      @click.stop="$emit('remove-detail')"
    >
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
  readonly: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits([
  'detail-drag-start',
  'drag-end',
  'click-detail',
  'remove-detail',
  'update-begin-time',
]);

const handleDragStart = (event) => {
  if (props.readonly) return;
  emit('detail-drag-start', event);
};

const handleDragEnd = () => {
  if (props.readonly) return;
  emit('drag-end');
};

const handleInputBeginTime = (event) => {
  if (props.readonly) return;
  
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