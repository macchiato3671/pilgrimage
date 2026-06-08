<template>
  <article
    class="plan-card"
    @click="$emit('select', plan)"
  >
    <p class="plan-title">
      {{ plan.title }}
    </p>

    <div class="button-group">
      <button
        type="button"
        class="edit-button"
        @click.stop="$emit('edit', plan)"
      >
        수정
      </button>

      <button
        type="button"
        class="delete-button"
        @click.stop="$emit('delete', plan)"
      >
        ×
      </button>
    </div>
  </article>
</template>

<script setup>
  defineProps({
    plan: {
      type: Object,
      required: true,
    },
  });

  defineEmits(['select', 'edit', 'delete']);
</script>

<style scoped>

</style>