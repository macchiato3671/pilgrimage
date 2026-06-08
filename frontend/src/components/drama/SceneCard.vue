<template> 
  <main class="component">
    <img class="thumbnail" :src="props.imgUrl" alt="씬 이미지"/>
    <p>{{ props.name }}</p>
    <p>{{ props.description }}</p>
    <p>{{ props.address }}</p>
    <p>{{ props.latitude }}, {{ props.longitude }}</p>
  </main>
</template>

<script setup>
  // PROPS
  const props = defineProps({
    componentW: String,
    componentH: String,

    name: String,
    description: String,
    address: String,
    latitude: String,
    longitude: String,
    imgUrl: String,
  })
</script>

<style lang="scss" scoped>
.component {
  width:  v-bind('props.componentW');
  height: v-bind('props.componentH');
  background-color: lightgray;
}

.thumbnail {
  width: 33%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
}
</style>