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
    import { ref } from 'vue';
    import { useRouter } from 'vue-router';
    import { useAuthStore } from '@/stores/authStore';
    import { makePlan } from '@/api/planApi';

    const router = useRouter();
    const authStore = useAuthStore();

    const activeTarget = ref('');
    const isCalendarOpen = ref(false);

    const title = ref('');
    const beginDate = ref('');
    const endDate = ref('');
    const isSubmitting = ref(false);

    const errorMessage = ref('');

    const openCalendar = (target) => {
        activeTarget.value = target;
        isCalendarOpen.value = true;
    }

    const closeCalendar = () => {
        isCalendarOpen.value = false;
    }

    const selectDate = (date) => {
        if (activeTarget.value === 'begin') {
            beginDate.value = date;
        }

        if (activeTarget.value === 'end') {
            endDate.value = date;
        }

        isCalendarOpen.value = false;
    }

    const handleCreatePlan = async () => {
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

        const requestBody = {
          title: title.value,
          beginDate: beginDate.value,
          endDate: endDate.value
        };

        const isLoggedIn = !!authStore.accessToken;

        if(isLoggedIn) {
          await createServerPlan(requestBody);
          return;
        }

        createLocalPlan(requestBody);
      } finally {
        isSubmitting.value = false;
      }
    }

    const createServerPlan = async (requestBody) => {
      try {
        const response = await makePlan(requestBody);
        const planId = response.planId;

        router.push({
          name: 'planDetail',
          params: { planId },
        });
      } catch (error) {
        const status = error.status;
        const errorCode = error.errorCode;
        
        if (status === 400) {
          if (errorCode === 'PLAN_TITLE_REQUIRED') {
            errorMessage.value = '제목을 입력해주세요.';
            return;
          }

          if (errorCode === 'PLAN_BEGIN_DATE_REQUIRED') {
            errorMessage.value = '시작일을 입력해주세요.';
            return;
          }

          if (errorCode === 'PLAN_END_DATE_REQUIRED') {
            errorMessage.value = '종료일을 입력해주세요.';
            return;
          }

          if (errorCode === 'PLAN_DATE_INVALID') {
            errorMessage.value = '오는 날은 가는 날보다 빠를 수 없습니다.';
            return;
          }

          errorMessage.value = error.message || '입력값을 다시 확인해주세요.';
          return;
        }

        if (status === 401) {
          errorMessage.value = '로그인이 필요합니다.';
          router.push('/login');
          return;
        }

        if (status === 403) {
          errorMessage.value = '여행 계획을 생성할 권한이 없습니다.';
          return;
        }

        if (status === 500) {
          errorMessage.value = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
          return;
        }

        errorMessage.value = '계획 생성에 실패했습니다.';
      }
    }

    const createLocalPlan = (requestBody) => {
      const localPlanId = `local-${Date.now()}`;

      const localPlan = {
        localPlanId,
        title: requestBody.title,
        beginDate: requestBody.beginDate,
        endDate: requestBody.endDate,
        details: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      }

      const savedPlans = JSON.parse(localStorage.getItem('localPlans') || '[]');
      savedPlans.push(localPlan);
      localStorage.setItem('localPlans', JSON.stringify(savedPlans));

      router.push({
        name: 'localPlanDetail',
        params: { localPlanId },
      });
}
</script>

<style scoped>
.error-message {
  color: red;
  font-size: 14px;
  margin-top: 8px;
}
</style>