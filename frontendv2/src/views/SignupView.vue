<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import brandMark from '../assets/images/brand-mark.png'
import dramaImage from '../assets/images/drama-perfect-crown.jpg'
import AppIcon from '../components/common/AppIcon.vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const form = reactive({ email: '', nickname: '', password: '', confirmPassword: '' })
const error = ref('')

const submit = async () => {
  error.value = ''
  if (form.password !== form.confirmPassword) return (error.value = '비밀번호가 일치하지 않습니다.')
  try {
    await auth.register({ email: form.email, nickname: form.nickname, password: form.password })
    const next = String(route.query.next || '/dramas')
    router.replace(auth.hasPendingGuestWork ? { name: 'sync', query: { next } } : next)
  } catch (reason) { error.value = reason.message }
}
</script>

<template>
  <main class="auth-page reverse">
    <section class="auth-visual" :style="{ backgroundImage: `linear-gradient(180deg,rgba(14,20,33,.08),rgba(14,20,33,.78)),url(${dramaImage})` }">
      <RouterLink class="auth-brand" to="/dramas"><img :src="brandMark" alt="" /><span><strong>필그리미지</strong><small>장면이 여행지가 되다</small></span></RouterLink>
      <div><span class="eyebrow light">START YOUR PILGRIMAGE</span><h1>첫 번째 촬영지를<br />저장해 보세요.</h1><p>회원가입 후 모든 기기에서 위시리스트와 여행 계획을 관리할 수 있습니다.</p></div>
    </section>
    <section class="auth-form-wrap">
      <form class="auth-form" @submit.prevent="submit">
        <div><span class="eyebrow">CREATE ACCOUNT</span><h2>회원가입</h2><p>필그리미지에서 사용할 기본 정보를 입력해 주세요.</p></div>
        <label class="field full"><span>이메일</span><input v-model.trim="form.email" type="email" autocomplete="email" required /></label>
        <label class="field full"><span>닉네임</span><input v-model.trim="form.nickname" maxlength="255" autocomplete="nickname" required /></label>
        <label class="field full"><span>비밀번호</span><input v-model="form.password" type="password" autocomplete="new-password" minlength="8" required /></label>
        <label class="field full"><span>비밀번호 확인</span><input v-model="form.confirmPassword" type="password" autocomplete="new-password" minlength="8" required /></label>
        <p v-if="error" class="form-error"><AppIcon name="alert" :size="16" />{{ error }}</p>
        <button class="button primary auth-submit" :disabled="auth.busy">{{ auth.busy ? '계정 생성 중…' : '회원가입' }}</button>
        <p class="auth-switch">이미 계정이 있나요? <RouterLink :to="{ name: 'login', query: route.query }">로그인</RouterLink></p>
      </form>
    </section>
  </main>
</template>
