<template>
  <main class="wishlist-page">
    <section class="wishlist-layout">
      <section
        class="map-section"
        :class="{ 'has-selected-scene': selectedScene }"
      >
        <SceneDetailCard
          v-if="selectedScene"
          :scene="selectedScene"
          @close="closeSelectedScene"
        />

        <MapComponent
          class="map"
          @ready="handleMapReady"
        />
      </section>

      <section class="wishlist-section">

        <p
          v-if="isLoading"
          class="status-message"
        >
          위시리스트를 불러오는 중입니다.
        </p>

        <p
          v-else-if="errorMessage"
          class="status-message error-message"
        >
          {{ errorMessage }}
        </p>

        <p
          v-else-if="wishlists.length === 0"
          class="status-message"
        >
          위시리스트가 비어 있습니다.
        </p>

        <div
          v-else
          class="wishlist-list"
        >
          <SceneCard
            v-for="wishlist in wishlists"
            :key="wishlist.wishlistId ?? wishlist.scene.sceneId"
            class="wishlist-card"

            :scene-id="wishlist.scene.sceneId"
            :name="wishlist.scene.name"
            :description="wishlist.scene.description"
            :address="wishlist.scene.address"
            :latitude="wishlist.scene.latitude"
            :longitude="wishlist.scene.longitude"
            :img-url="wishlist.scene.imgUrl"
            :is-wishlisted="true"

            @toggle-wishlist="handleRemoveWishlist"
            @view-detail="handleViewDetail"
          />
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
  import { computed, nextTick, onMounted, ref } from 'vue';
  import MapComponent from '@/components/common/MapComponent.vue';
  import SceneCard from '@/components/drama/SceneCard.vue';
  import SceneDetailCard from '@/components/scene/SceneDetailCard.vue';
  import { getWishlist, deleteWishlist } from '@/api/wishlistApi';
  import { useAuthStore } from '@/stores/authStore';

  const authStore = useAuthStore();

  const wishlists = ref([]);
  const isLoading = ref(false);
  const errorMessage = ref('');

  const selectedScene = ref(null);

  const mapSdk = ref(null);
  const map = ref(null);
  const markers = ref([]);

  const scenes = computed(() => {
    return wishlists.value
      .map((wishlist) => wishlist.scene)
      .filter((scene) => scene);
  });

  onMounted(() => {
    fetchData();
  });

  const fetchData = async () => {
    isLoading.value = true;
    errorMessage.value = '';

    try {
      if(authStore.isLoggedIn) {
        await fetchServerData();
      } else {
        fetchLocalData();
      }

      renderMarkers();
    } catch(error) {
      console.error(error);
      errorMessage.value = '위시리스트를 불러오지 못했습니다.';
    } finally {
      isLoading.value = false;
    }
  };

  const fetchServerData = async() => {
    const response = await getWishlist();

    wishlists.value = response.wishlists ?? [];
  };

  const fetchLocalData = () => {
    const localWishlists = JSON.parse(localStorage.getItem('wishlists')) || [];
    wishlists.value = localWishlists;
  };

  const handleRemoveWishlist = async ({ sceneId }) => {
    const isConfirmed = confirm('위시리스트에서 삭제하시겠습니까?');

    if (!isConfirmed) return;

    try {
      if (authStore.isLoggedIn) {
        await deleteWishlist(sceneId);
      }

      wishlists.value = wishlists.value.filter((wishlist) => {
        return String(wishlist.scene.sceneId) !== String(sceneId);
      });

      if (!authStore.isLoggedIn) {
        localStorage.setItem('wishlists', JSON.stringify(wishlists.value));
      }

      clearSelectedSceneIfDeleted(sceneId);

      await nextTick();

      if (map.value) {
        map.value.relayout();
      }

      if (selectedScene.value) {
        renderSelectedMarker(selectedScene.value);
      } else {
        renderMarkers();
      }
    } catch (error) {
      console.error(error);
      alert('위시리스트 삭제에 실패했습니다.');
    }
  };

  const clearSelectedSceneIfDeleted = (sceneId) => {
    if (!selectedScene.value) return;

    if (String(selectedScene.value.sceneId) === String(sceneId)) {
      selectedScene.value = null;
    }
  };

  const handleViewDetail = async ({ sceneId }) => {
    const targetWishlist = wishlists.value.find((wishlist) => {
      return String(wishlist.scene.sceneId) === String(sceneId);
    });

    if (!targetWishlist) return;

    selectedScene.value = targetWishlist.scene;

    await nextTick();

    if (map.value) {
      map.value.relayout();
    }

    renderSelectedMarker(targetWishlist.scene);
  };

  const renderSelectedMarker = (scene) => {
    if (!mapSdk.value || !map.value) return;

    clearMarkers();

    const position = createPosition(scene);

    if (!position) return;

    const kakao = mapSdk.value;
    const kakaoMap = map.value;

    const marker = new kakao.maps.Marker({
      map: kakaoMap,
      position,
    });

    markers.value.push(marker);

    kakaoMap.setCenter(position);
    kakaoMap.setLevel(3);
  };

  const createPosition = (scene) => {
    const lat = Number(scene.latitude);
    const lng = Number(scene.longitude);

    if (Number.isNaN(lat) || Number.isNaN(lng)) {
      return null;
    }

    return new mapSdk.value.maps.LatLng(lat, lng);
  };

  const closeSelectedScene = async () => {
    selectedScene.value = null;

    await nextTick();

    if (map.value) {
      map.value.relayout();
    }

    renderMarkers();
  };

  const handleMapReady = (payload) => {
    mapSdk.value = payload._mapSdk ?? payload.kakao;
    map.value = payload._map ?? payload.map;

    renderMarkers();
  };

  const clearMarkers = () => {
    markers.value.forEach((marker) => {
      marker.setMap(null);
    });

    markers.value = [];
  };

  const renderMarkers = () => {
    if (!mapSdk.value || !map.value) return;

    clearMarkers();

    if (!scenes.value.length) return;

    const kakao = mapSdk.value;
    const kakaoMap = map.value;

    const bounds = new kakao.maps.LatLngBounds();

    scenes.value.forEach((scene) => {
      const position = createPosition(scene);

      if (!position) return;

      const marker = new kakao.maps.Marker({
        map: kakaoMap,
        position,
      });

      markers.value.push(marker);
      bounds.extend(position);
    });

    if (markers.value.length === 0) return;

    if (markers.value.length === 1) {
      kakaoMap.setCenter(markers.value[0].getPosition());
      kakaoMap.setLevel(3);
      return;
    }

    kakaoMap.setBounds(bounds);
  };
</script>

<style scoped>
.wishlist-page {
  width: 100%;
  padding: 12px 0 32px;
  box-sizing: border-box;
}

.wishlist-layout {
  display: flex;
  align-items: flex-start;
  gap: 16px;

  width: 100%;
  box-sizing: border-box;
}

.map-section {
  width: 420px;
  min-width: 420px;

  display: flex;
  flex-direction: column;
}

.map {
  width: 100%;
  height: 640px;

  flex: none;
  min-height: 0;

  border: 1px solid #111;
  box-sizing: border-box;
}

.map-section.has-selected-scene .map {
  height: 260px;
  min-height: 260px;
}

.wishlist-section {
  width: 430px;
  min-width: 430px;
  height: 640px;

  padding: 10px;
  border: 1px solid #111;
  box-sizing: border-box;
}

.wishlist-list {
  display: flex;
  flex-direction: column;
  gap: 10px;

  height: 100%;
  overflow-y: auto;
}

.status-message {
  margin-top: 36px;
  text-align: center;
  font-size: 14px;
}

.error-message {
  color: red;
}
</style>