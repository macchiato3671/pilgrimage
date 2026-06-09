<template>
<article class="scene-detail-card">
    <div class="detail-header">
      <h2 class="detail-title">상세 정보</h2>

      <button
        type="button"
        class="close-button"
        @click="$emit('close')"
      >
        ×
      </button>
    </div>

    <img
      class="scene-image"
      :src="scene.imgUrl"
      :alt="scene.name"
    />

    <div class="detail-content">
      <h3 class="scene-name">
        {{ scene.name }}
      </h3>

      <p class="scene-address">
        {{ scene.address }}
      </p>

      <p class="scene-description">
        {{ scene.description || '등록된 설명이 없습니다.' }}
      </p>

      <p class="scene-coordinate">
        위도 {{ scene.latitude }} / 경도 {{ scene.longitude }}
      </p>
    </div>
  </article>
</template>

<script setup>
  defineProps({
    scene: {
      type: Object,
      required: true,
    },
  });

  defineEmits(['close']);
</script>

<style scoped>
.scene-detail-card {
  width: 100%;
  height: 380px;

  border: 1px solid #111;
  border-bottom: none;
  box-sizing: border-box;

  overflow: hidden;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  height: 42px;
  padding: 0 12px;

  border-bottom: 1px solid #111;
  box-sizing: border-box;
}

.detail-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}

.close-button {
  width: 28px;
  height: 28px;

  border: 1px solid #111;
  background: none;

  font-size: 18px;
  line-height: 1;
  cursor: pointer;
}

.scene-image {
  width: 100%;
  height: 150px;

  display: block;
  object-fit: cover;

  border-bottom: 1px solid #111;
}

.detail-content {
  padding: 12px;
  box-sizing: border-box;
}

.scene-name {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 700;
}

.scene-address,
.scene-description,
.scene-coordinate {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.4;
}
</style>