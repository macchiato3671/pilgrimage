<template>
  <div>
    <h1>로그인</h1>

    <form v-on:submit.prevent="handleSignin">
      <div>
        <label>이메일</label>
        <input
          type="email"
          v-model="form.email"
          placeholder="이메일을 입력하세요"
        />
      </div>

      <div>
        <label>비밀번호</label>
        <input
          type="password"
          v-model="form.password"
          placeholder="비밀번호를 입력하세요"
        />
      </div>

      <p v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </p>

      <button type="submit" :disabled="isLoading">
        {{ isLoading ? '로그인 중...' : '로그인' }}
      </button>
    </form>
    
    <div>
      <span>아직 회원이 아니신가요?</span>
      <RouterLink to="/signup">회원가입</RouterLink>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { signin } from '@/api/memberApi'
import { useAuthStore } from '@/stores/authStore';

const router = useRouter();
const authStore = useAuthStore();

const form = reactive({
  email: '',
  password: '',
})

const errorMessage = ref('')
const isLoading = ref(false)

const handleSignin = async () => {
  errorMessage.value = ''

  if (!form.email || !form.password) {
    errorMessage.value = '모든 항목을 입력해주세요.'
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (!emailRegex.test(form.email)) {
    errorMessage.value = '올바른 이메일 형식이 아닙니다.'
    return 
  }

  const requestBody = {
    email: form.email,
    password: form.password
  }

  try {
    isLoading.value = true

    const data = await signin(requestBody)

    if (!data?.accessToken) {
      errorMessage.value = '로그인 응답에 토큰이 없습니다.'
      return
    }
    
    authStore.login(data)
    console.log('로그인 성공:', data)
    alert('로그인이 완료되었습니다.')

    router.push('/search')
  } catch (error) {
    console.error('로그인 실패:', error)

    if(!error.status){
      errorMessage.value = '서버와 연결할 수 없습니다.'
      return;
    }

    const errorCode = error.errorCode

    if (errorCode === 'INVALID_EMAIL') {
      errorMessage.value = '이메일 형식이 올바르지 않습니다.'
    } else if (errorCode === 'INVALID_PASSWORD') {
      errorMessage.value = '비밀번호 형식이 올바르지 않습니다.'
    } else if (errorCode === 'INVALID_CREDENTIALS') {
      errorMessage.value = '이메일 또는 비밀번호가 올바르지 않습니다.'
    } else if (errorCode === 'MEMBER_ACCESS_DENIED') {
      errorMessage.value = '탈퇴 또는 정지된 계정입니다.'
    } else {
      errorMessage.value = '로그인 중 오류가 발생했습니다.'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.error-message {
  color: red;
}
</style>