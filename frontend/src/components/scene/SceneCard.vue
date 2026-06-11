<template> 
  <article class="component">
    <img class="thumbnail" :src="imgUrl" :alt='`${name} 씬 이미지`'/>
    <div class="details">
      <p>이름: {{ name }}</p>
      <p>주소: {{ address }}</p>
    </div>
    <div class="interactions">
      <button
        type="button"
        :aria-pressed="isWishlisted"
        :title="isWishlisted ? '위시리스트에서 제거' : '위시리스트에 추가'"
        :disabled="isWishlistPending"
        @click="toggleWishlist"
      >
        {{isWishlisted ? '🌟' : '⭐'}}
      </button>
      <button
        type="button"
        title="상세 보기"
        @click="viewDetail"
      >
        👀
      </button>
    </div>
  </article>
</template>

<script setup>
// PROPS
const props = defineProps({
  sceneId: {
    type: [String, Number],
    required: true
  },
  name: {
    type: String,
    required: true,
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
  isWishlistPending: {
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
  width:  100%;
  height: auto;
  background-color: lightgray;
  display: flex;
  gap: 8px;
}

.thumbnail {
  flex: 33;
  aspect-ratio: 16 / 9;
  object-fit: cover;
  min-width: 0;
}

.details {
  flex: 57;
  min-width: 0;
}

.interactions {
  flex: 10;
  min-width: 0;

  display: flex;
  flex-direction: column;
}
</style>
