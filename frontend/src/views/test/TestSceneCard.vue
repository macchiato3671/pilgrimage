<template>
  <main>
    <div v-for="scene in scenes" :key="scene.sceneId">
      <SceneCard
        :width="componentW"
        :height="componentH"

        :scene-id="scene.sceneId"
        :name="scene.name"
        :description="scene.description"
        :address="scene.address"
        :latitude="scene.latitude"
        :longitude="scene.longitude"
        :img-url="scene.imgUrl"
        :is-wishlisted="scene.isWishlisted"

        @toggle-wishlist="handleToggleWishlist"
        @view-detail="handleViewDetail"
      />
      <br/>
    </div>
  </main>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import SceneCard from '@/components/drama/SceneCard.vue'

const router = useRouter()

const componentW = ref('100%')
const componentH = ref('auto')

const scenes = ref([
  {
    sceneId: 0,
    name: '샌즈 전',
    description: '와! 언더테일 아시는구나! 샌즈 정.말.쎕.니.다.',
    address: 'Last Corridor, Underground, Mt Ebott',
    latitude: 15.578884844,
    longitude: 888.48441557,
    imgUrl: 'https://i.namu.wiki/i/vi6nCucR_X3W6DSW4loXSpfaxk-bx272VqXH2dh0iaYjVyWRO88VhL-A7dhuycby59svXTy7TA88418oitJWszUkcGyzimBc02bmZ99P9MmE8cbLa8BkwXVwZFWMfPCk-4twrPGl2OYizgG6aPjtwg.png',
    isWishlisted: true,
  },
  {
    sceneId: 1,
    name: '토토로 숲길',
    description: '토토로가 나올 것 같은 조용한 숲길입니다.',
    address: 'Saitama, Japan',
    latitude: 35.799,
    longitude: 139.468,
    isWishlisted: false,
  },
  {
    sceneId: 2,
    name: '너의 이름은 계단',
    description: '타키와 미츠하가 마주친 계단 느낌의 장소입니다.',
    address: 'Tokyo, Japan',
    latitude: 35.685,
    longitude: 139.730,
    imgUrl: 'https://ak-d.tripcdn.com/images/1i66h2215dbd1iz837F8E_R_600_400_R5_Q90.jpg?proc=source/trip',
    isWishlisted: false,
  },
])

const handleToggleWishlist = ({ sceneId }) => {
  const targetScene = scenes.value.find(scene => scene.sceneId === sceneId)

  if (!targetScene) return

  targetScene.isWishlisted = !targetScene.isWishlisted
}

const handleViewDetail = ({ sceneId }) => {
  router.push(`/scenes/${sceneId}`)
}
</script>