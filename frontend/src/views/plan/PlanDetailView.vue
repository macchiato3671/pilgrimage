<template>
  <main class="plan-detail-view">
    <section class="map-section">
      <MapComponent
        class="map"
        @ready="handleMapReady"
      />
    </section>

    <section class="schedule-column">
      <div class="detail-actions">
        <button type="button" @click="goPlanEdit">
          수정
        </button>
      </div>
      <PlanSchedule
        v-if="plan"
        :draft-title="plan.title"
        :draft-date-text="planDateText"
        :days="days"
        :active-day-no="activeDayNo"
        :current-day-details="currentDayDetails"
        readonly
        @change-day="changeActiveDay"
        @click-detail="handleClickDetail"
      />
    </section>
  </main>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

import PlanSchedule from '@/components/plan/PlanSchedulePanel.vue'
import MapComponent from '@/components/common/MapComponent.vue'
import { planService } from '@/services/planService';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const plan = ref(null)
const activeDayNo = ref(1)
const map = ref(null)
const markers = ref([])
const mapSdk = ref(null);

const planDateText = computed(() => {
  if (!plan.value) return ''

  return `${plan.value.beginDate} ~ ${plan.value.endDate}`
})

const formatDayDate = (date) => {
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const days = computed(() => {
  if (!plan.value) return []

  const begin = new Date(plan.value.beginDate)
  const end = new Date(plan.value.endDate)
  const dayMs = 24 * 60 * 60 * 1000
  const tripDays = Math.floor((end - begin) / dayMs) + 1

  return Array.from({ length: tripDays }, (_, index) => {
    const date = new Date(begin)
    date.setDate(begin.getDate() + index)

    return {
      dayNo: index + 1,
      dateText: formatDayDate(date),
    }
  })
})

const currentDayDetails = computed(() => {
  if (!plan.value?.details) return []

  return plan.value.details
    .filter((detail) => Number(detail.dayNo) === Number(activeDayNo.value))
    .map(normalizeDetail)
    .sort((a, b) => a.beginTime.localeCompare(b.beginTime))
})

const normalizeDetail = (detail) => {
  const target = detail.place ?? detail.scene ?? detail

  return {
    ...detail,
    tempId: detail.planDetailId ?? `${detail.dayNo}-${detail.beginTime}-${target.id ?? target.placeId ?? target.sceneId}`,
    name: target.name,
    address: target.address,
    latitude: target.latitude,
    longitude: target.longitude,
  }
}

const changeActiveDay = (dayNo) => {
  activeDayNo.value = dayNo

  nextTick(() => {
    renderMarkers(currentDayDetails.value)
  })
}

const handleMapReady = ({ _mapSdk, _map }) => {
  mapSdk.value = _mapSdk
  map.value = _map

  nextTick(() => {
    renderMarkers(currentDayDetails.value)
  })
}

const handleClickDetail = (detail) => {
  if (!map.value || !mapSdk.value) return
  if (detail.latitude == null || detail.longitude == null) return

  const position = new mapSdk.value.maps.LatLng(detail.latitude, detail.longitude)

  map.value.setLevel(4)
  map.value.setCenter(position)
}

const clearMarkers = () => {
  markers.value.forEach((marker) => marker.setMap(null))
  markers.value = []
}

const renderMarkers = (details) => {
  if (!map.value || !mapSdk.value) return

  clearMarkers()

  const validDetails = details.filter((detail) => {
    return (
      Number.isFinite(Number(detail.latitude)) &&
      Number.isFinite(Number(detail.longitude))
    )
  })

  if (validDetails.length === 0) return

  const bounds = new mapSdk.value.maps.LatLngBounds()

  validDetails.forEach((detail) => {
    const position = new mapSdk.value.maps.LatLng(
      Number(detail.latitude),
      Number(detail.longitude),
    )

    const marker = new mapSdk.value.maps.Marker({
      position,
      map: map.value,
    })

    markers.value.push(marker)
    bounds.extend(position)
  })

  if (validDetails.length === 1) {
    const detail = validDetails[0]

    map.value.setLevel(4)
    map.value.setCenter(
      new mapSdk.value.maps.LatLng(
        Number(detail.latitude),
        Number(detail.longitude),
      ),
    )

    return
  }

  map.value.setBounds(bounds)
}

const fetchPlan = async () => {
  try {
    const planId = route.params.planId;

    const response = await planService.fetchDetail({
      planId: planId,
      isLoggedIn: authStore.isLoggedIn,
    });
    plan.value = response.plan;

    activeDayNo.value = 1

    await nextTick()
    renderMarkers(currentDayDetails.value)
  } catch (error) {
    console.error(error)
    alert('여행 일정 조회에 실패했습니다.')
  }
}

const goPlanEdit = () => {
    router.push({
      name: 'planEdit',
      params: {
        planId: route.params.planId,
      },
    });
    return;
  };

onMounted(() => {
  fetchPlan();
});
</script>

<style scoped>
.plan-detail-view {
  display: grid;
  grid-template-columns: 1fr 420px;
  height: calc(100vh - 64px);
}

.map-section {
  min-width: 0;
}

.map {
  width: 100%;
  height: 100%;
}

.schedule-column {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
}
</style>
