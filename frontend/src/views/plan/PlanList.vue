<template>
  <main class="plan-list-page">
    <h1 class="page-title">여행 계획 페이지</h1>

    <section class="plan-list-box">
      <button
        type="button"
        class="create-button"
        @click="goCreatePlan"
      >
        새 여행 계획하기
      </button>

      <p v-if="isLoading" class="status-message">
        여행 계획을 불러오는 중입니다.
      </p>

      <p v-else-if="errorMessage" class="status-message error-message">
        {{ errorMessage }}
      </p>

      <p v-else-if="plans.length === 0" class="status-message">
        아직 등록된 여행 계획이 없습니다.
      </p>

      <template v-else>
        <PlanCard
          v-for="plan in plans"
          :key="plan.planId ?? plan.localPlanId"
          :plan="plan"
          @select="goPlanDetail"
          @edit="goPlanEdit"
          @delete="handleDeletePlan"
        />
      </template>
    </section>
  </main>
</template>

<script setup>
  import { onMounted, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import PlanCard from '@/components/plan/PlanCard.vue';
  import { getPlans, deletePlan } from '@/api/planApi';
  import { useAuthStore } from '@/stores/authStore';

  const router = useRouter();
  const authStore = useAuthStore();

  const plans = ref([]);
  const isLoading = ref(false);
  const errorMessage = ref('');

  onMounted(() => {
    fetchPlans();
  });

  const fetchPlans = async () => {
    isLoading.value = true;
    errorMessage.value = '';

    try{
      if(authStore.isLoggedIn) {
        const response = await getPlans();
        plans.value = response.plans ?? [];
        return;
      }

      const localPlans = JSON.parse(localStorage.getItem('localPlans')) || [];
      plans.value = localPlans;
    } catch (error) {
      const status = error.status;

      if (status === 401) {
        errorMessage.value = '로그인이 필요합니다.';
        return;
      }

      if (status === 403) {
        errorMessage.value = '여행 일정을 조회할 권한이 없습니다.';
        return;
      }

      if (status === 500) {
        errorMessage.value = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        return;
      }

      errorMessage.value = '여행 일정 목록을 불러오지 못했습니다.';
    } finally {
      isLoading.value = false;
    }
  };

  const goCreatePlan = () => {
    router.push({
      name: 'planCreate',
    });
  };

  const goPlanDetail = (plan) => {
    if (authStore.isLoggedIn) {
      router.push({
        name: 'planDetail',
        params: {
          planId: plan.planId,
        },
      });
      return;
    }

    router.push({
      name: 'localPlanDetail',
      params: {
        localPlanId: plan.localPlanId,
      },
    });
  };

  const goPlanEdit = (plan) => {
    if (authStore.isLoggedIn) {
      router.push({
        name: 'planEdit',
        params: {
          planId: plan.planId,
        },
      });
      return;
    }

    router.push({
      name: 'localPlanEdit',
      params: {
        localPlanId: plan.localPlanId,
      },
    });
  };

  const handleDeletePlan = async (plan) => {
    const isConfirmed = confirm('여행 계획을 삭제하시겠습니까?');

    if (!isConfirmed) return;

    try {
      if(authStore.isLoggedIn) {
        await deletePlan(plan.planId);

        plans.value = plans.value.filter((item) => item.planId !== plan.planId);
        return;
      }
      const nextPlans = plans.value.filter((item) => item.localPlanId !== plan.localPlanId);

      localStorage.setItem('localPlans', JSON.stringify(nextPlans));
      plans.value = nextPlans;
    } catch (error) {
      const status = error.status;
      if (status === 401) {
        alert('로그인이 필요합니다.');
        router.push({ name: 'login' });
        return;
      }

      if (status === 403) {
        alert('해당 여행 계획을 삭제할 권한이 없습니다.');
        return;
      }

      if (status === 404) {
        alert('이미 삭제되었거나 존재하지 않는 여행 계획입니다.');

        // 서버에는 없는데 화면에 남아있는 경우 화면에서 제거
        plans.value = plans.value.filter((item) => item.planId !== plan.planId);
        return;
      }

      if (status === 500) {
        alert('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        return;
      }

      alert('여행 계획 삭제에 실패했습니다.');
    }
  }
</script>

<style lang="scss" scoped>

</style>