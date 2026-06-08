<template>
  <main class='main'>
    <MapComponent 
      class="map"
      @ready="handleMapReady"
    />
    <div class="scene-list">
      <SceneCard
        v-for="scene in scenes"
        :key="scene.sceneId"
        class="scene-card"

        :scene-id="scene.sceneId"
        :name="scene.name"
        :description="scene.description"
        :address="scene.address"
        :latitude="scene.latitude"
        :longitude="scene.longitude"
        :img-url="scene.imgUrl"

        @toggle-wishlist="handleToggleWishlist"
        @view-detail="handleViewDetail"
      />
    </div>
  </main>
</template>

<script setup>
import { fetchSceneList } from '@/api/sceneApi';
import MapComponent from '@/components/common/MapComponent.vue';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import SceneCard from '@/components/drama/SceneCard.vue'

// ROUTE
const route = useRoute()
const router = useRouter()
const dramaId = route.params.dramaId

// DATA
const mapSdk = ref(null)
const map = ref(null)
const scenes = ref([])

// METHODS
const handleMapReady = ({ _mapSdk, _map  }) => {
  mapSdk.value = _mapSdk
  map.value = _map
}
const fetchData = async () => {
  const response = await fetchSceneList(dramaId)
  scenes.value = response.scenes
  console.log(scenes.value)
}
const handleToggleWishlist = ({ sceneId }) => {
  console.log(sceneId)
}
const handleViewDetail = ({ sceneId }) => {
  router.push(`/scenes/${sceneId}`)
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

.map {
  flex: 1;
  height: 80vh;
  border: 1px solid black;
}

.scene-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;

  height: 80vh;
  min-height: 0;
  border: 1px solid black;

  overflow-y: auto;
}

.scene-card {
  width: 100%;
}
</style>