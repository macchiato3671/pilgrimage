<template>
  <div>
    <AppHeader />

    <main>
      <slot />
    </main>
  </div>
</template>

<script setup>
import AppHeader from './AppHeader.vue'
</script>

<style scoped>
main {
  width: 80vw;
  min-height: calc(100vh - 68px);
}
</style>