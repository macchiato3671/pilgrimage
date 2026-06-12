<template>
  <main>
    <section>
      <form v-on:submit.prevent="handleCreatePlan">
        <p>언제 얼마나 떠나세요?</p>

        <input
          v-model="title"
          type="text"
          placeholder="여행 제목"
        />

        <button type="button" @click="openCalendar('begin')">
          {{ beginDate || '가는 날' }}
        </button>

        <button type="button" @click="openCalendar('end')">
          {{ endDate || '오는 날' }}
        </button>

        <p v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </p>

        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '생성 중...' : '계획 세우러 떠나기' }}
        </button>
      </form>
    </section>

    <section
    v-if="isCalendarOpen"
    class="calendar-overlay"
    >
      <div class="calendar-modal">
        <h3>
        {{ activeTarget === 'begin' ? '가는 날 선택' : '오는 날 선택' }}
        </h3>

        <input
          type="date"
          @change="selectDate($event.target.value)"
        />

        <button @click="closeCalendar">
        닫기
        </button>
      </div>
    </section>
  </main>
</template>

<script setup>
  import { ref, onMounted, computed } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { useAuthStore } from '@/stores/authStore';
  import { planService } from '@/services/planService';
  import { getPlanDraft, setPlanBaseInfo } from '@/utils/planDraftStorage';

  const route = useRoute();
  const router = useRouter();
  const authStore = useAuthStore();

  const activeTarget = ref('');
  const isCalendarOpen = ref(false);

  const title = ref('');
  const beginDate = ref('');
  const endDate = ref('');
  const isSubmitting = ref(false);
  const errorMessage = ref('');

  const isEditMode = computed(() => 
    route.name === 'planEdit'
  );

  onMounted(async () => {
    if (isEditMode.value) {
      const response = await planService.fetchDetail({
        planId: route.params.planId, 
        isLoggedIn: authStore.isLoggedIn
      });
      title.value = response.plan.title;
      beginDate.value = response.plan.beginDate;
      endDate.value = response.plan.endDate;

      setPlanBaseInfo({
        title: response.plan.title,
        beginDate: response.plan.beginDate,
        endDate: response.plan.endDate,
      });

      return;
    }
    const draft = getPlanDraft();

    if (!draft) return;

    title.value = draft.title;
    beginDate.value = draft.beginDate;
    endDate.value = draft.endDate;
  });

  const openCalendar = (target) => {
    activeTarget.value = target;
    isCalendarOpen.value = true;
  };

  const closeCalendar = () => {
    isCalendarOpen.value = false;
  };

  const selectDate = (date) => {
    if (activeTarget.value === 'begin') {
      beginDate.value = date;
    }

    if (activeTarget.value === 'end') {
      endDate.value = date;
    }

    isCalendarOpen.value = false;
  };

  const handleCreatePlan = () => {
    if (isSubmitting.value) {
      return;
    }

    isSubmitting.value = true;
    errorMessage.value = '';

    try{
      if(!title.value.trim()){
        errorMessage.value = '제목을 입력해주세요.';
        return;
      }

      if(!beginDate.value){
        errorMessage.value = '시작일을 입력해주세요.';
        return;
      }

      if(!endDate.value){
        errorMessage.value = '종료일을 입력해주세요.';
        return;
      }

      if(beginDate.value > endDate.value){
        errorMessage.value = '오는 날은 가는 날보다 빠를 수 없습니다.';
        return;
      }

      setPlanBaseInfo({
        title: title.value.trim(),
        beginDate: beginDate.value,
        endDate: endDate.value,
      });

      router.push({
        name: isEditMode.value ? 'planDetailEdit' : 'planDetailCreate',
        params: isEditMode.value ? { planId: route.params.planId } : {},
      })
    } finally {
      isSubmitting.value = false;
    }
  }
</script>

<style scoped>
.error-message {
  color: red;
  font-size: 14px;
  margin-top: 8px;
}
</style>