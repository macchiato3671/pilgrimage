<script setup>
import { reactive, ref, watch } from 'vue'
import { PLAN_COLORS } from '../../config/app'
import { today } from '../../models/date'
import { validatePlan } from '../../models/plan'
import AppIcon from '../common/AppIcon.vue'
import BaseModal from '../common/BaseModal.vue'

const props = defineProps({ open: Boolean, plan: Object })
const emit = defineEmits(['close', 'submit'])

const plusDays = (date, count) => new Date(new Date(`${date}T00:00:00`).getTime() + count * 86400000).toISOString().slice(0, 10)
const blank = () => ({ title: '', beginDate: today(), endDate: plusDays(today(), 2), memo: '', color: PLAN_COLORS[0] })
const form = reactive(blank())
const error = ref('')

watch(
  () => [props.open, props.plan],
  () => {
    Object.assign(form, props.plan ? { ...blank(), ...props.plan } : blank())
    error.value = ''
  },
  { deep: true },
)

const submit = () => {
  error.value = validatePlan(form)
  if (!error.value) emit('submit', { ...form })
}
</script>

<template>
  <BaseModal :open="open" :title="plan ? '여행 정보 수정' : '새 여행 계획'" width="560px" @close="emit('close')">
    <form class="plan-form" @submit.prevent="submit">
      <label class="field full"><span>여행 제목</span><input v-model="form.title" maxlength="255" placeholder="예: 서울 드라마 촬영지 여행" autofocus /></label>
      <label class="field"><span>시작일</span><input v-model="form.beginDate" type="date" /></label>
      <label class="field"><span>종료일</span><input v-model="form.endDate" type="date" :min="form.beginDate" /></label>
      <fieldset class="field full color-field">
        <legend>핀 색상 <small>지도와 일정에서만 사용되며 서버에는 전송하지 않습니다.</small></legend>
        <div class="color-options">
          <button
            v-for="color in PLAN_COLORS"
            :key="color"
            type="button"
            :class="{ selected: form.color === color }"
            :style="{ '--swatch': color }"
            :aria-label="color"
            @click="form.color = color"
          />
          <label class="custom-color">직접 선택 <input v-model="form.color" type="color" /></label>
        </div>
      </fieldset>
      <label class="field full"><span>메모</span><textarea v-model="form.memo" rows="4" placeholder="이번 여행에서 기억할 내용을 적어 보세요." /></label>
      <p v-if="error" class="form-error"><AppIcon name="alert" :size="16" />{{ error }}</p>
      <div class="form-actions"><button type="button" class="button secondary" @click="emit('close')">취소</button><button class="button primary">확인</button></div>
    </form>
  </BaseModal>
</template>
