<template>
  <article
    class="candidate-card"
    draggable="true"
    @dragstart="$emit('candidate-drag-start', $event)"
    @dragend="$emit('drag-end')"
    @click="$emit('click-candidate')"
  >
    <h3>{{ item.name }}</h3>
    <p>{{ item.address }}</p>

    <p v-if="showDistance && item.distanceKm !== null">
      {{ item.distanceKm }}km
    </p>

    <button
      v-if="showNearbyButton"
      type="button"
      @click.stop="$emit('show-nearby-attractions')"
    >
      주변 관광지 보기
    </button>
  </article>
</template>

<script setup>
defineProps({
  item: {
    type: Object,
    required: true,
  },
  showNearbyButton: {
    type: Boolean,
    default: false,
  },
  showDistance: {
    type: Boolean,
    default: false,
  },
});

defineEmits([
  'candidate-drag-start',
  'drag-end',
  'click-candidate',
  'show-nearby-attractions',
]);
</script>

<style scoped>
.candidate-card {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
}

.candidate-card:hover {
  background-color: #f5f5f5;
}
</style>