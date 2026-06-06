<template>
  <header>
    <nav class="left-menu">
      <RouterLink to="/drama">Pilgrimage</RouterLink>
      <RouterLink to="/drama">드라마 찾기</RouterLink>
      <RouterLink to="/wishlist">위시리스트</RouterLink>   
      <RouterLink to="/plans">여행계획</RouterLink>
    </nav>

    <form @submit.prevent="handleSearch">
      <input 
        type="text"
        v-model="keyword"
        placeholder="검색어를 입력하세요"
      />
      <button type="submit">🔎</button>
    </form>

    <nav class="right-menu">
      <template v-if="authStore.isLoggedIn">
        <button type="button" @click="handleLogout">로그아웃</button>
        <RouterLink to="/mypage">마이페이지</RouterLink>
      </template>

      <template v-else>
        <RouterLink to="/login">로그인</RouterLink>
      </template>
    </nav>
  </header>
</template>

<script setup>
  import { ref } from 'vue'
  import { RouterLink, useRouter } from 'vue-router'
  import { useAuthStore } from '@/stores/authStore'

  const router = useRouter()
  const authStore = useAuthStore();

  const keyword = ref('')


  const handleSearch = () => {
    const keywordTrim = keyword.value.trim();

    if (!keywordTrim) {
      return
    }

    router.push({
      path: '/search',
      query: {
        keyword: keywordTrim
      }
    })
  }

  const handleLogout = () => {
    authStore.logout();
    router.push('/login');
  }
</script>

<style scoped>

</style>