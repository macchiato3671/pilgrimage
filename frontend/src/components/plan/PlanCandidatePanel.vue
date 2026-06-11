<template>
  <section class="candidate-section">
    <div class="tab-buttons">
      <button
        type="button"
        :class="{ active: activeTab === 'wish' }"
        @click="$emit('change-tab', 'wish')"
      >
        wish
      </button>

      <button
        type="button"
        :class="{ active: activeTab === 'tour' }"
        @click="$emit('change-tab', 'tour')"
      >
        tour
      </button>
    </div>

    <section v-if="activeTab === 'wish'" class="candidate-list">
      <p v-if="wishItems.length === 0">
        위시리스트가 없습니다.
      </p>

      <PlanCandidateCard
        v-for="item in wishItems"
        :key="item.key"
        :item="item"
        :show-nearby-button="true"
        @candidate-drag-start="$emit('candidate-drag-start', item, $event)"
        @drag-end="$emit('drag-end')"
        @click-candidate="$emit('click-candidate', item)"
        @show-nearby-attractions="$emit('show-nearby-attractions', item)"
      />
    </section>

    <section v-if="activeTab === 'tour'" class="candidate-list">
      <p v-if="selectedWishItem">
        {{ selectedWishItem.name }} 주변 관광지
      </p>

      <p v-if="isTourLoading">
        주변 관광지를 불러오는 중입니다.
      </p>

      <p v-else-if="tourItems.length === 0">
        {{ tourMessage }}
      </p>

      <PlanCandidateCard
        v-for="item in tourItems"
        :key="item.key"
        :item="item"
        :show-distance="true"
        @candidate-drag-start="$emit('candidate-drag-start', item, $event)"
        @drag-end="$emit('drag-end')"
        @click-candidate="$emit('click-candidate', item)"
      />
    </section>
  </section>
</template>

<script setup>
import PlanCandidateCard from './PlanCandidateCard.vue';

defineProps({
  activeTab: {
    type: String,
    required: true,
  },
  wishItems: {
    type: Array,
    required: true,
  },
  tourItems: {
    type: Array,
    required: true,
  },
  selectedWishItem: {
    type: Object,
    default: null,
  },
  isTourLoading: {
    type: Boolean,
    required: true,
  },
  tourMessage: {
    type: String,
    required: true,
  },
});

defineEmits([
  'change-tab',
  'candidate-drag-start',
  'drag-end',
  'click-candidate',
  'show-nearby-attractions',
]);
</script>

<style scoped>
.candidate-section {
  padding: 16px;
  overflow-y: auto;
}

.tab-buttons {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tab-buttons button.active {
  font-weight: 700;
}

.candidate-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>