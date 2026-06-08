<template>
  <main>
    <h1>임시 홈</h1>

    <button @click="goPlanCreate">
      여행계획 세우러 가기
    </button>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goPlanCreate = () => {
  router.push({ name: 'planCreate' })
}
</script>

<style scoped>
main {
  padding: 40px;
}

button {
  padding: 12px 20px;
  cursor: pointer;
}
</style>