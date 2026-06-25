<script setup>
import { ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({ modelValue: String, placeholder: { type: String, default: '검색어를 입력하세요' }, busy: Boolean })
const emit = defineEmits(['update:modelValue', 'search'])
const value = ref(props.modelValue || '')
watch(() => props.modelValue, (next) => (value.value = next || ''))
const submit = () => {
  emit('update:modelValue', value.value)
  emit('search', value.value)
}
</script>

<template>
  <form class="search-box" @submit.prevent="submit">
    <AppIcon name="search" :size="19" />
    <input
      v-model="value"
      :placeholder="placeholder"
      @input="emit('update:modelValue', value)"
      @keyup.esc="value = ''; submit()"
    />
    <button class="search-submit" :disabled="busy">{{ busy ? '검색 중' : '검색' }}</button>
  </form>
</template>
