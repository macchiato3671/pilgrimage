<template>
  <main>
    <SceneCard 
      :component-w = 'componentW'
      :component-h = 'componentH'

      name = "샌즈 전"
      description = "와! 언더테일 아시는구나! 샌즈 정.말.쎕.니.다."
      address = "Last Corridor, Underground, Mt Ebott"
      latitude="15.578884844"
      longitude="888.48441557"
      img-url = "https://i.namu.wiki/i/vi6nCucR_X3W6DSW4loXSpfaxk-bx272VqXH2dh0iaYjVyWRO88VhL-A7dhuycby59svXTy7TA88418oitJWszUkcGyzimBc02bmZ99P9MmE8cbLa8BkwXVwZFWMfPCk-4twrPGl2OYizgG6aPjtwg.png"
    />
  </main>
</template>

<script setup>
import SceneCard from '@/components/drama/SceneCard.vue'
import { ref } from 'vue'

const componentW = ref('400px')
const componentH = ref('200px')
</script>
