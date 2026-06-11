<template>
  <main class="plan-detail-create">
    <section class="map-section">
      <MapComponent
        class="map"
        @ready="handleMapReady"
      />
    </section>

    <PlanSchedulePanel
      :draft-title="draftTitle"
      :draft-date-text="draftDateText"
      :days="days"
      :active-day-no="activeDayNo"
      :current-day-details="currentDayDetails"
      :is-saving="isSaving"
      @cancel="handleCancel"
      @save="handleSavePlan"
      @change-day="changeActiveDay"
      @drop-to-schedule="handleDropToSchedule"
      @detail-drag-start="handleDetailDragStart"
      @drag-end="handleDragEnd"
      @click-detail="handleClickScheduleDetail"
      @remove-detail="removeDetail"
      @update-begin-time="updateDetailBeginTime"
    />

    <PlanCandidatePanel
      :active-tab="activeTab"
      :wish-items="wishItems"
      :tour-items="tourItems"
      :selected-wish-item="selectedWishItem"
      :is-tour-loading="isTourLoading"
      :tour-message="tourMessage"
      @change-tab="changeTab"
      @candidate-drag-start="handleCandidateDragStart"
      @drag-end="handleDragEnd"
      @click-candidate="handleClickCandidate"
      @show-nearby-attractions="handleShowNearbyAttractions"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import MapComponent from '@/components/common/MapComponent.vue';
import PlanSchedulePanel from '@/components/plan/PlanSchedulePanel.vue';
import PlanCandidatePanel from '@/components/plan/PlanCandidatePanel.vue';

import { getPlanDraft, clearPlanDraft } from '@/utils/planDraftStorage';
import { useAuthStore } from '@/stores/authStore';
import { getWishlist } from '@/api/wishlistApi';
import { localApiClient } from '@/api/localClient';
import { getNearbyAttractions } from '@/api/sceneApi';
import { makePlan } from '@/api/planApi';

const router = useRouter();
const authStore = useAuthStore();

const mapSdk = ref(null);
const map = ref(null);
const markers = ref([]);

const draft = ref(null);
const wishItems = ref([]);
const activeTab = ref('wish');

const activeDayNo = ref(1);
const draggedItem = ref(null);
const isDragging = ref(false);
const details = ref([]);

const tourItems = ref([]);
const selectedWishItem = ref(null);
const isTourLoading = ref(false);
const tourMessage = ref('위시리스트를 선택하면 주변 관광지가 표시됩니다.');

const isSaving = ref(false);

const draftTitle = computed(() => {
  return draft.value?.title ?? '';
});

const draftDateText = computed(() => {
  if (!draft.value) return '';

  return `${draft.value.beginDate} ~ ${draft.value.endDate}`;
});

const days = computed(() => {
  if (!draft.value?.beginDate || !draft.value?.endDate) return [];

  const begin = new Date(`${draft.value.beginDate}T00:00:00`);
  const end = new Date(`${draft.value.endDate}T00:00:00`);
  const dayMs = 24 * 60 * 60 * 1000;

  const dayCount = Math.floor((end - begin) / dayMs) + 1;

  return Array.from({ length: dayCount }, (_, index) => ({
    dayNo: index + 1,
  }));
});

const currentDayDetails = computed(() => {
  return getCurrentDayDetails();
});

const timeToMinutes = (time) => {
  if (!time) return null;

  const [hour, minute] = time.split(':').map(Number);

  if (!Number.isInteger(hour) || !Number.isInteger(minute)) {
    return null;
  }

  return hour * 60 + minute;
};

const minutesToTime = (minutes) => {
  const safeMinutes = Math.max(0, Math.min(minutes, 23 * 60 + 59));

  const hour = Math.floor(safeMinutes / 60);
  const minute = safeMinutes % 60;

  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
};

const roundToNearestFiveMinutes = (minutes) => {
  return Math.round(minutes / 5) * 5;
};

const getCurrentDayDetails = (excludeTempId = null) => {
  return details.value
    .filter((detail) => detail.dayNo === activeDayNo.value)
    .filter((detail) => detail.tempId !== excludeTempId)
    .slice()
    .sort((a, b) => {
      const aMinutes = timeToMinutes(a.beginTime) ?? Number.MAX_SAFE_INTEGER;
      const bMinutes = timeToMinutes(b.beginTime) ?? Number.MAX_SAFE_INTEGER;

      return aMinutes - bMinutes;
    });
};

const updateDetailBeginTime = ({ tempId, beginTime }) => {
  const target = details.value.find((detail) => {
    return detail.tempId === tempId;
  });

  if (!target) return;

  target.beginTime = beginTime;
};

onMounted(async () => {
  draft.value = getPlanDraft();

  if (!draft.value) {
    router.replace({
      name: 'planCreate',
    });
    return;
  }

  await loadWishlists();
});

const loadWishlists = async () => {
  try {
    const response = authStore.isLoggedIn
      ? await getWishlist()
      : await localApiClient.get('/wishlist');

    const wishlists = response.wishlists ?? [];

    wishItems.value = wishlists.map(normalizeWishlistItem);

    renderWishlistMarkers();
  } catch (error) {
    console.error(error);
  }
};

const handleShowNearbyAttractions  = async (item) => {
  if (isDragging.value) return;

  moveMapToItem(item);

  selectedWishItem.value = item;
  activeTab.value = 'tour';
  isTourLoading.value = true;
  tourMessage.value = '';

  try {
    const response = await getNearbyAttractions(item.sceneId, {
      radiusKm: 3,
      page: 0,
      size: 10,
    });

    const attractions = response.attractions ?? [];

    tourItems.value = attractions.map(normalizeTourItem);

    if (tourItems.value.length === 0) {
      tourMessage.value = '주변 관광지가 없습니다.';
    }

    renderTourMarkers();
  } catch (error) {
    console.error(error);
    tourItems.value = [];
    tourMessage.value = '주변 관광지를 불러오지 못했습니다.';
    clearMarkers();
  } finally {
    isTourLoading.value = false;
  }
};

const normalizeWishlistItem = (wishlist) => {
  const scene = wishlist.scene ?? wishlist;

  return {
    key: `scene-${scene.sceneId}`,
    sceneId: scene.sceneId,
    placeId: null,
    name: scene.name,
    address: scene.address,
    latitude: Number(scene.latitude),
    longitude: Number(scene.longitude),
    imgUrl: scene.imgUrl,
    raw: wishlist,
  };
};

const normalizeTourItem = (attraction) => {
  return {
    key: `place-${attraction.placeId}`,
    sceneId: null,
    placeId: attraction.placeId,
    name: attraction.name,
    description: attraction.description,
    address: attraction.address,
    latitude: Number(attraction.latitude),
    longitude: Number(attraction.longitude),
    imgUrl: attraction.imgUrl,
    contentId: attraction.contentId,
    contentTypeId: attraction.contentTypeId,
    contentTypeName: attraction.contentTypeName,
    distanceKm: attraction.distanceKm ?? null,
    raw: attraction,
  };
};

const handleMapReady = ({ _mapSdk, _map }) => {
  mapSdk.value = _mapSdk;
  map.value = _map;

  renderWishlistMarkers();
};

const clearMarkers = () => {
  markers.value.forEach((marker) => {
    marker.setMap(null);
  });

  markers.value = [];
};

const renderMarkers = (items, options = {}) => {
  if (!map.value || !mapSdk.value) return;

  const shouldFitBounds = options.fitBounds ?? true;

  const currentCenter = map.value.getCenter();
  const currentLevel = map.value.getLevel();

  clearMarkers();

  const validItems = items.filter((item) => {
    return Number.isFinite(item.latitude) && Number.isFinite(item.longitude);
  });

  if (validItems.length === 0) return;

  const bounds = new mapSdk.value.maps.LatLngBounds();

  validItems.forEach((item) => {
    const position = new mapSdk.value.maps.LatLng(
      item.latitude,
      item.longitude,
    );

    const marker = new mapSdk.value.maps.Marker({
      map: map.value,
      position,
    });

    markers.value.push(marker);
    bounds.extend(position);
  });

  if (!shouldFitBounds) {
    map.value.setLevel(currentLevel);
    map.value.setCenter(currentCenter);
    return;
  }

  if (validItems.length === 1) {
    const item = validItems[0];

    map.value.setLevel(4);
    map.value.setCenter(
      new mapSdk.value.maps.LatLng(item.latitude, item.longitude),
    );

    return;
  }

  map.value.setBounds(bounds);
};

const renderWishlistMarkers = (options = {}) => {
  renderMarkers(wishItems.value, options);
};

const renderTourMarkers = (options = {}) => {
  renderMarkers(tourItems.value, options);
};

const renderScheduleMarkers = (options = {}) => {
  renderMarkers(currentDayDetails.value, options);
};

const moveMapToItem = (item) => {
  if (!map.value || !mapSdk.value) return;

  if (
    !Number.isFinite(item.latitude) ||
    !Number.isFinite(item.longitude)
  ) {
    return;
  }

  const position = new mapSdk.value.maps.LatLng(
    item.latitude,
    item.longitude,
  );

  map.value.setLevel(4);
  map.value.setCenter(position);
};

const changeTab = (tab) => {
  activeTab.value = tab;

  if (tab === 'wish') {
    renderWishlistMarkers();
    return;
  }

  if (tab === 'tour') {
    renderTourMarkers();
  }
};

const changeActiveDay = (dayNo) => {
  activeDayNo.value = dayNo;
  renderScheduleMarkers();
};

const handleClickScheduleDetail = (detail) => {
  if (isDragging.value) return;

  renderScheduleMarkers({ fitBounds: false });
  moveMapToItem(detail);
};

const handleClickCandidate = (item) => {
  if (isDragging.value) return;

  if (activeTab.value === 'wish') {
    renderWishlistMarkers({ fitBounds: false });
  }

  if (activeTab.value === 'tour') {
    renderTourMarkers({ fitBounds: false });
  }

  moveMapToItem(item);
};

const handleCandidateDragStart = (item, event) => {
  isDragging.value = true;

  draggedItem.value = {
    type: 'candidate',
    item,
  };

  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy';
    event.dataTransfer.setData('text/plain', item.key);
  }
};

const handleDetailDragStart = (detail, event) => {
  isDragging.value = true;

  draggedItem.value = {
    type: 'detail',
    detail,
  };

  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', detail.tempId);
  }
};

const handleDragEnd = () => {
  setTimeout(() => {
    isDragging.value = false;
    draggedItem.value = null;
  }, 0);
};

const hasValidTarget = (item) => {
  const hasScene = item.sceneId !== null && item.sceneId !== undefined;
  const hasPlace = item.placeId !== null && item.placeId !== undefined;

  return hasScene !== hasPlace;
};

const handleDropToSchedule = (event) => {
  if (!draggedItem.value) return;

  if (draggedItem.value.type === 'candidate') {
    addDetailFromCandidate(event);
    return;
  }

  if (draggedItem.value.type === 'detail') {
    moveDetailInSchedule(event);
  }
};

const addDetailFromCandidate = (event) => {
  const item = draggedItem.value.item;

  if (!hasValidTarget(item)) {
    draggedItem.value = null;
    return;
  }

  const insertIndex = getDropInsertIndex(event);

  details.value.push({
    tempId: `detail-${Date.now()}-${Math.random()}`,
    dayNo: activeDayNo.value,
    sceneId: item.sceneId,
    placeId: item.placeId,
    name: item.name,
    address: item.address,
    latitude: item.latitude,
    longitude: item.longitude,
    beginTime: getSuggestedBeginTime(insertIndex),
  });

  draggedItem.value = null;
  renderScheduleMarkers();
};

const moveDetailInSchedule = (event) => {
  const detail = draggedItem.value.detail;

  const insertIndex = getDropInsertIndex(event, detail.tempId);
  const nextBeginTime = getSuggestedBeginTime(insertIndex, detail.tempId);

  const targetDetail = details.value.find((item) => {
    return item.tempId === detail.tempId;
  });

  if (!targetDetail) {
    draggedItem.value = null;
    return;
  }

  targetDetail.dayNo = activeDayNo.value;
  targetDetail.beginTime = nextBeginTime;

  draggedItem.value = null;
  renderScheduleMarkers();
};

const getDropInsertIndex = (event, excludeTempId = null) => {
  const cards = Array.from(
    event.currentTarget.querySelectorAll('.schedule-card'),
  ).filter((card) => {
    return card.dataset.tempId !== excludeTempId;
  });

  const dropY = event.clientY;

  const index = cards.findIndex((card) => {
    const rect = card.getBoundingClientRect();
    const middleY = rect.top + rect.height / 2;

    return dropY < middleY;
  });

  if (index === -1) {
    return cards.length;
  }

  return index;
};

const getSuggestedBeginTime = (insertIndex, excludeTempId = null) => {
  const dayDetails = getCurrentDayDetails(excludeTempId);

  const prevDetail = dayDetails[insertIndex - 1];
  const nextDetail = dayDetails[insertIndex];

  const prevMinutes = timeToMinutes(prevDetail?.beginTime);
  const nextMinutes = timeToMinutes(nextDetail?.beginTime);

  if (prevMinutes !== null && nextMinutes !== null && nextMinutes > prevMinutes) {
    const middleMinutes = roundToNearestFiveMinutes((prevMinutes + nextMinutes) / 2);
    return minutesToTime(middleMinutes);
  }

  if (prevMinutes !== null) {
    return minutesToTime(prevMinutes + 60);
  }

  if (nextMinutes !== null) {
    return minutesToTime(nextMinutes - 60);
  }

  return '09:00';
};

const removeDetail = (tempId) => {
  details.value = details.value.filter((detail) => {
    return detail.tempId !== tempId;
  });

  renderScheduleMarkers();
};

const handleCancel = () => {
  router.back();
};

const buildPlanCreateRequest = () => {
  return {
    title: draft.value.title,
    beginDate: draft.value.beginDate,
    endDate: draft.value.endDate,
    details: details.value
      .slice()
      .sort((a, b) => {
        if (a.dayNo !== b.dayNo) {
          return a.dayNo - b.dayNo;
        }

        const aMinutes = timeToMinutes(a.beginTime) ?? Number.MAX_SAFE_INTEGER;
        const bMinutes = timeToMinutes(b.beginTime) ?? Number.MAX_SAFE_INTEGER;

        return aMinutes - bMinutes;
      })
      .map((detail) => ({
        dayNo: detail.dayNo,
        sceneId: detail.sceneId,
        placeId: detail.placeId,
        beginTime: detail.beginTime,
      })),
  };
};

const validatePlanSave = () => {
  if (!draft.value) {
    return '여행 정보가 없습니다.';
  }

  if (details.value.length === 0) {
    return '일정에 목적지를 하나 이상 추가해주세요.';
  }

  const hasInvalidTarget = details.value.some((detail) => {
    return !hasValidTarget(detail);
  });

  if (hasInvalidTarget) {
    return '잘못된 일정 항목이 있습니다.';
  }

  const hasEmptyTime = details.value.some((detail) => {
    return !detail.beginTime;
  });

  if (hasEmptyTime) {
    return '시작 시간이 비어 있는 일정이 있습니다.';
  }

  return '';
};

const handleSavePlan = async () => {
  if (isSaving.value) return;

  const errorMessage = validatePlanSave();

  if (errorMessage) {
    alert(errorMessage);
    return;
  }

  isSaving.value = true;

  try {
    const requestBody = buildPlanCreateRequest();

    if (authStore.isLoggedIn) {
      await makePlan(requestBody);
    } else {
      await localApiClient.post('/plans', requestBody);
    }

    clearPlanDraft();

    router.replace({
      name: 'planList',
    });
  } catch (error) {
    console.error(error);
    alert('여행 일정 저장에 실패했습니다.');
  } finally {
    isSaving.value = false;
  }
};
</script>

<style scoped>
.plan-detail-create {
  display: grid;
  grid-template-columns: 1.2fr 1fr 320px;
  height: 100vh;
}

.map-section {
  min-width: 0;
  height: 100%;
}

.map {
  width: 100%;
  height: 100%;
}
</style>