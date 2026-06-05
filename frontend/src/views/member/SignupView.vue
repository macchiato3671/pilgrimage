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
const successMessage = ref('')
const isLoading = ref(false)

const handleSignup = async () => {
  errorMessage.value = ''

  if (!form.nickname || !form.email || !form.password || !form.passwordConfirm) {
    errorMessage.value = '모든 항목을 입력해주세요.'
    return
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (!emailRegex.test(form.email)) {
    return '올바른 이메일 형식이 아닙니다.'
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
    successMessage.value = '회원가입이 완료되었습니다.'

    form.nickname = ''
    form.email = ''
    form.password = ''
    form.passwordConfirm = ''

    alert('회원가입이 완료되었습니다.')
    router.push('/login')
  } catch (error) {
    console.error('회원가입 실패:', error)
    errorMessage.value = error.message || '회원가입에 실패했습니다.'
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