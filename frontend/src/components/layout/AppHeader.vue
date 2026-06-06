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
      <template v-if="isLoggedIn">
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
  import { ref, watch } from 'vue'
  import { RouterLink, useRoute, useRouter } from 'vue-router'

  const router = useRouter()
  const route = useRoute()

  const keyword = ref('')
  const isLoggedIn = ref(!!localStorage.getItem('accessToken'))

  const updateLoginState = () => {
    isLoggedIn.value = !!localStorage.getItem('accessToken')
  }

  watch(
    () => route.fullPath,
    () => {
      updateLoginState()
    }
  )

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
    localStorage.removeItem('accessToken')
    updateLoginState()
    router.push('/login')
  }
</script>

<style scoped>

</style>