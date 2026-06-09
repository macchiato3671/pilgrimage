<template> 
  <article class="component">
    <img class="thumbnail" :src="imgUrl" :alt='`${name} 씬 이미지`'/>
    <br/>
    <h2>{{ name }}</h2>
    <p>주소: {{ address }}</p>
    <br/>
    <p>설명: {{ description }}</p>
    <br/>
    <p>위도: {{ latitude }}, 경도: {{ longitude }}</p>
    <br/>
    <label>돌아가기 </label>
    <button @click="emit('exitDetail')">❌</button>
    <label>{{isWishlisted ? '  찜풀기 ' : '  찜하기 '}}</label>
    <button @click="emit('toggleWishlist', {
      _sceneId: sceneId,
      _isWishlisted: isWishlisted,
    })">
      {{isWishlisted ? '🌟' : '⭐'}}
    </button>
  </article>
</template>

<script setup>
// PROPS
defineProps({
  sceneId: {
    type: [String, Number],
    required: true
  },
  name: {
    type: String,
    required: true,
  },
  description: {
    type: String,
    default: '',
  },
  address: {
    type: String,
    default: '',
  },
  latitude: {
    type: [String, Number],
    default: '',
  },
  longitude: {
    type: [String, Number],
    default: '',
  },
  imgUrl: {
    type: String,
    default: import.meta.env.VITE_NO_IMAGE_URL,
  },

  isWishlisted: {
    type: Boolean,
    default: false,
  },
})

// EMIT
const emit = defineEmits(['exitDetail', 'toggleWishlist'])
</script>

<style lang="scss" scoped>
.component {
  height: 100%;
  background-color: lightgray;
}

.thumbnail {
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  min-width: 0;
}
</style>