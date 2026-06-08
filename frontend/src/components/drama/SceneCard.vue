<template> 
  <article class="component">
    <img class="thumbnail" :src="imgUrl" :alt='`${name} 씬 이미지`'/>
    <p>이름: {{ name }}</p>
    <p>설명: {{ description }}</p>
    <p>주소: {{ address }}</p>
    <p>위도, 경도: {{ latitude }}, {{ longitude }}</p>
    <div>
      <button @click="toggleWishlist">{{isWishlisted ? '🌟' : '⭐'}}</button>
      <button @click="viewDetail">👀</button>
    </div>
  </article>
</template>

<script setup>
// PROPS
const props = defineProps({
  width: {
    type: String,
    default: '600px',
  },
  height: {
    type: String,
    default: 'auto',
  },

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

// DATA


// EMITS
const emit = defineEmits([
  'toggle-wishlist',
  'view-detail',
])

// METHODS
const toggleWishlist = () => emit('toggle-wishlist', {
  sceneId: props.sceneId,
  isWishlisted: props.isWishlisted
})
const viewDetail = () => emit('view-detail', {
  sceneId: props.sceneId,
})
</script>

<style lang="scss" scoped>
.component {
  width:  v-bind('props.width');
  height: v-bind('props.height');
  background-color: lightgray;
}

.thumbnail {
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: cover;
}
</style>