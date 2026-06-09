<template>
  <main class='main'>
    <section class="section-left">
      <SceneDetail
        v-if="selectedScene"
        class="detail"
        @exit-detail="handleExitDetail"
        @toggle-wishlist="handleToggleWishlist"

        :scene-id="selectedScene.sceneId"
        :name="selectedScene.name"
        :address="selectedScene.address"
        :latitude="selectedScene.latitude"
        :longitude="selectedScene.longitude"
        :img-url="selectedScene.imgUrl"

        :is-wishlisted="isWished(selectedScene.sceneId)"
      />
      <MapComponent 
        class="map"
        @ready="handleMapReady"
      />
    </section>
    <div class="scene-list">
      <SceneCard
        v-for="scene in scenes"
        :key="scene.sceneId"
        class="scene-card"

        :scene-id="scene.sceneId"
        :name="scene.name"
        :address="scene.address"
        :latitude="scene.latitude"
        :longitude="scene.longitude"
        :img-url="scene.imgUrl"

        :is-wishlisted="isWished(scene.sceneId)"

        @toggle-wishlist="handleToggleWishlist"
        @view-detail="handleViewDetail"
      />
    </div>
  </main>
</template>

<script setup>
import { fetchSceneList } from '@/api/sceneApi';
import MapComponent from '@/components/common/MapComponent.vue';
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import SceneCard from '@/components/scene/SceneCard.vue'
import SceneDetail from '@/components/scene/SceneDetail.vue';
import { addWishlist, deleteWishlist, getWishlist } from '@/api/wishlistApi';
import { useAuthStore } from '@/stores/authStore';

// ROUTE
const route = useRoute()
const dramaId = route.params.dramaId

// DATA
const mapSdk = ref(null)
const map = ref(null)
const scenes = ref([])
const markers = ref([])
const selectedSceneId = ref(null)
const wishedSceneIds = ref([])

const auth = useAuthStore()

// COMPUTED
const selectedScene = computed(() => {
  return scenes.value.find((scene) => {
    return String(scene.sceneId) === String(selectedSceneId.value)
  })
})

// METHODS
const handleMapReady = ({ _mapSdk, _map  }) => {
  mapSdk.value = _mapSdk
  map.value = _map

  renderMarkers()
}
const clearMarkers = () => {
  markers.value.forEach((marker) => {
    marker.setMap(null)
  })
  markers.value = []
}
const renderMarkers = () => {
  if (!mapSdk.value || !map.value) return
  if (!scenes.value.length) return

  const kakao = mapSdk.value
  const kakaoMap = map.value

  clearMarkers()

  const bounds = new kakao.maps.LatLngBounds()

  scenes.value.forEach((scene) => {
    const lat = Number(scene.latitude)
    const lng = Number(scene.longitude)

    if (Number.isNaN(lat) || Number.isNaN(lng)) return

    const position = new kakao.maps.LatLng(lat, lng)

    const marker = new kakao.maps.Marker({
      map: kakaoMap,
      position,
    })

    markers.value.push(marker)
    bounds.extend(position)
  })

  if (markers.value.length === 0) return

  if (markers.value.length === 1) {
    kakaoMap.setCenter(markers.value[0].getPosition())
    kakaoMap.setLevel(3)
    return
  }

  kakaoMap.setBounds(bounds)
}
const renderSelectedMarker = () => {
  if (!mapSdk.value || !map.value) return
  if (!selectedScene.value) return

  const kakao = mapSdk.value
  const kakaoMap = map.value

  clearMarkers()

  const lat = Number(selectedScene.value.latitude)
  const lng = Number(selectedScene.value.longitude)
  if (Number.isNaN(lat) || Number.isNaN(lng)) return

  const position = new kakao.maps.LatLng(lat, lng)
  const marker = new kakao.maps.Marker({
    map: kakaoMap,
    position,
  })
  markers.value.push(marker)

  kakaoMap.setCenter(markers.value[0].getPosition())
  kakaoMap.setLevel(6)
}
const fetchData = async () => {
  await fetchScenes()
  await fetchWishes()
}
const fetchScenes = async () => {
  const response = await fetchSceneList(dramaId)
  scenes.value = response.scenes
  console.log(scenes.value)

  renderMarkers()
}
const fetchWishes = async () => {
  if (auth.isLoggedIn) {
    const response = await getWishlist()
    console.log(response)
    wishedSceneIds.value = response.wishlists.map((wish) => String(wish.sceneId))
  }
  else { // TODO: fetch wishes from localStorage if not logged in
    console.error("Not Implemented")
  }
}

const isWished = sceneId => wishedSceneIds.value.includes(String(sceneId))
const addWishSceneId = (sceneId) => {
  const id = String(sceneId)
  if (wishedSceneIds.value.includes(id)) return
  wishedSceneIds.value.push(id)
}
const deleteWishSceneId = (sceneId) => {
  const id = String(sceneId)
  wishedSceneIds.value = wishedSceneIds.value.filter(wishedSceneId => wishedSceneId !== id)
}

const handleToggleWishlist = async ({ sceneId, isWishlisted }) => {
  if (auth.isLoggedIn) {
    if (isWishlisted) {
      await deleteWishlist(sceneId)
      deleteWishSceneId(sceneId)
    }
    else {
      await addWishlist(sceneId)
      addWishSceneId(sceneId)
    }
  }
  else { // TODO: manage wishes from localStorage if not logged in

  }
}
const handleViewDetail = async ({ sceneId }) => {
  selectedSceneId.value = sceneId
  renderSelectedMarker()
}
const handleExitDetail = () => {
  selectedSceneId.value = null
  renderMarkers()
}

// ON MOUNT
onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.main {
  display: flex;
  width: 100%;
  gap: 16px;
}

.section-left {
  flex: 1;
  min-width: 0;
  height: 80vh;
  border: 1px solid black;

  display: flex;
  flex-direction: column;
  gap: 8px
}
.detail {
  flex: 2 1 0;
  min-width: 0;
  border: 1px solid black;
}
.map {
  flex: 1 1 0;
  min-width: 0;
  border: 1px solid black;
}

.scene-list {
  flex: 1 1 0;
  min-width: 0;

  display: flex;
  flex-direction: column;
  gap: 8px;

  height: 80vh;
  min-height: 0;
  border: 1px solid black;

  overflow-y: auto;
}

.scene-card {
  width: 100%;
}
</style>