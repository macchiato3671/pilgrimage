<template>
  <div>
    <h1>회원가입</h1>

    <form v-on:submit.prevent="handleSignup">
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

      <div>
        <label>비밀번호 확인</label>
        <input
          type="password"
          v-model="form.passwordConfirm"
          placeholder="비밀번호를 다시 입력하세요"
        />
      </div>

      <div>
        <label>닉네임</label>
        <input
          type="text"
          v-model="form.nickname"
          placeholder="닉네임을 입력하세요"
        />
      </div>

      <p v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </p>

      <button type="submit" :disabled="isLoading">
        {{ isLoading ? '가입 중...' : '가입하기' }}
      </button>
    </form>

    <button type="button" @click="goBack">
      ‹ Go back
    </button>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { signup } from '@/api/memberApi'

const router = useRouter()

const form = reactive({
  nickname: '',
  email: '',
  password: '',
  passwordConfirm: ''
})

const errorMessage = ref('')
const isLoading = ref(false)

const goBack = () => {
  router.back()
}

const handleSignup = async () => {
  errorMessage.value = ''

  if (!form.nickname || !form.email || !form.password || !form.passwordConfirm) {
    errorMessage.value = '모든 항목을 입력해주세요.'
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (!emailRegex.test(form.email)) {
    errorMessage.value = '올바른 이메일 형식이 아닙니다.'
    return 
  }
  
  if (form.password !== form.passwordConfirm) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.'
    return
  }

  const requestBody = {
    nickname: form.nickname,
    password: form.password,
    email: form.email
  }

  try {
    isLoading.value = true

    const data = await signup(requestBody)

    console.log('회원가입 성공:', data)

    form.nickname = ''
    form.email = ''
    form.password = ''
    form.passwordConfirm = ''

    alert('회원가입이 완료되었습니다.')
    router.push('/login')
  } catch (error) {
    console.error('회원가입 실패:', error)
    
    if (!error.status) {
      errorMessage.value = '서버와 연결할 수 없습니다.'
      return
    }

    const errorCode = error.errorCode

    if (errorCode === 'EMAIL_ALREADY_EXISTS') {
      errorMessage.value = '이미 가입된 이메일입니다.'
    } else if (errorCode === 'INVALID_EMAIL') {
      errorMessage.value = '이메일 형식이 올바르지 않습니다.'
    } else if (errorCode === 'INVALID_PASSWORD') {
      errorMessage.value = '비밀번호 형식이 올바르지 않습니다.'
    } else if (errorCode === 'INVALID_NICKNAME') {
      errorMessage.value = '닉네임 형식이 올바르지 않습니다.'
    } else if (errorCode === 'REQUIRED_FIELD_MISSING') {
      errorMessage.value = '필수 입력값을 모두 입력해주세요.'
    } else {
      errorMessage.value = '회원가입 중 오류가 발생했습니다.'
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