<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import brandMark from '../assets/images/brand-mark.png'
import dramaImage from '../assets/images/drama-tangerines.jpg'
import AppIcon from '../components/common/AppIcon.vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const form = reactive({ email: '', password: '' })
const error = ref('')
const testEmail = import.meta.env.VITE_TEST_EMAIL
const testPassword = import.meta.env.VITE_TEST_PASSWORD

const fillTest = () => { form.email = testEmail || ''; form.password = testPassword || '' }
const submit = async () => {
  error.value = ''
  try {
    await auth.login(form)
    const next = String(route.query.next || '/dramas')
    router.replace(auth.hasPendingGuestWork ? { name: 'sync', query: { next } } : next)
  } catch (reason) { error.value = reason.message }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-visual" :style="{ backgroundImage: `linear-gradient(180deg,rgba(14,20,33,.08),rgba(14,20,33,.78)),url(${dramaImage})` }">
      <RouterLink class="auth-brand" to="/dramas"><img :src="brandMark" alt="" /><span><strong>필그리미지</strong><small>장면이 여행지가 되다</small></span></RouterLink>
      <div><span class="eyebrow light">YOUR SCENE, YOUR JOURNEY</span><h1>좋아한 장면을<br />직접 걸어보세요.</h1><p>촬영지를 저장하고 나만의 드라마 여행 일정을 완성하세요.</p></div>
    </section>
    <section class="auth-form-wrap">
      <form class="auth-form" @submit.prevent="submit">
        <div><span class="eyebrow">WELCOME BACK</span><h2>로그인</h2><p>계정에 저장된 위시리스트와 여행 계획을 이어서 관리하세요.</p></div>
        <label class="field full"><span>이메일</span><input v-model.trim="form.email" type="email" autocomplete="email" placeholder="member@example.com" required /></label>
        <label class="field full"><span>비밀번호</span><input v-model="form.password" type="password" autocomplete="current-password" placeholder="비밀번호" required /></label>
        <p v-if="error" class="form-error"><AppIcon name="alert" :size="16" />{{ error }}</p>
        <button class="button primary auth-submit" :disabled="auth.busy">{{ auth.busy ? '로그인 중…' : '로그인' }}</button>
        <button v-if="testEmail" type="button" class="button secondary" @click="fillTest">테스트 계정 채우기</button>
        <p class="auth-switch">아직 계정이 없나요? <RouterLink :to="{ name: 'signup', query: route.query }">회원가입</RouterLink></p>
        <RouterLink class="back-link" to="/dramas"><AppIcon name="back" :size="16" />비회원으로 둘러보기</RouterLink>
      </form>
    </section>
  </main>
</template>
